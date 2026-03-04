package com.inductiveautomation.ignition.examples.python3.gateway;

import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.concurrent.ExecutionException;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for PriorityExecutionRequest.
 *
 * @since v2.16.0 (Phase 3 Week 3-4)
 */
public class PriorityExecutionRequestTest {

    @Test
    public void testConstruction() {
        Map<String, Object> vars = new HashMap<>();
        vars.put("x", 10);

        PriorityExecutionRequest request = new PriorityExecutionRequest(
                "result = x + 1",
                vars,
                ExecutionPriority.HIGH
        );

        assertEquals("result = x + 1", request.getCode());
        assertEquals(vars, request.getVariables());
        assertEquals(ExecutionPriority.HIGH, request.getPriority());
        assertNotNull(request.getFuture());
        assertTrue(request.getTimestamp() > 0);
    }

    @Test
    public void testDefaultPriority() {
        PriorityExecutionRequest request = new PriorityExecutionRequest("code", null, null);
        assertEquals(ExecutionPriority.NORMAL, request.getPriority(),
                "Null priority should default to NORMAL");
    }

    @Test
    public void testComparison_DifferentPriorities() {
        PriorityExecutionRequest high = new PriorityExecutionRequest("code", null, ExecutionPriority.HIGH);
        PriorityExecutionRequest normal = new PriorityExecutionRequest("code", null, ExecutionPriority.NORMAL);
        PriorityExecutionRequest low = new PriorityExecutionRequest("code", null, ExecutionPriority.LOW);

        // HIGH < NORMAL < LOW (lower value = higher priority)
        assertTrue(high.compareTo(normal) < 0, "HIGH should come before NORMAL");
        assertTrue(normal.compareTo(low) < 0, "NORMAL should come before LOW");
        assertTrue(high.compareTo(low) < 0, "HIGH should come before LOW");

        // Reverse comparisons
        assertTrue(normal.compareTo(high) > 0);
        assertTrue(low.compareTo(normal) > 0);
        assertTrue(low.compareTo(high) > 0);
    }

    @Test
    public void testComparison_SamePriority_FIFO() throws InterruptedException {
        PriorityExecutionRequest first = new PriorityExecutionRequest("code1", null, ExecutionPriority.NORMAL);
        Thread.sleep(1); // Ensure different timestamps
        PriorityExecutionRequest second = new PriorityExecutionRequest("code2", null, ExecutionPriority.NORMAL);

        // Same priority - earlier timestamp should come first (FIFO)
        assertTrue(first.compareTo(second) < 0, "Earlier request should come first within same priority");
        assertTrue(second.compareTo(first) > 0);
    }

    @Test
    public void testPriorityQueue_Ordering() {
        PriorityQueue<PriorityExecutionRequest> queue = new PriorityQueue<>();

        // Add in reverse priority order
        queue.add(new PriorityExecutionRequest("low", null, ExecutionPriority.LOW));
        queue.add(new PriorityExecutionRequest("normal", null, ExecutionPriority.NORMAL));
        queue.add(new PriorityExecutionRequest("high", null, ExecutionPriority.HIGH));

        // Should come out in priority order (HIGH, NORMAL, LOW)
        assertEquals(ExecutionPriority.HIGH, queue.poll().getPriority());
        assertEquals(ExecutionPriority.NORMAL, queue.poll().getPriority());
        assertEquals(ExecutionPriority.LOW, queue.poll().getPriority());
    }

    @Test
    public void testPriorityQueue_MixedPriorities() {
        PriorityQueue<PriorityExecutionRequest> queue = new PriorityQueue<>();

        // Add multiple requests with mixed priorities
        queue.add(new PriorityExecutionRequest("normal1", null, ExecutionPriority.NORMAL));
        queue.add(new PriorityExecutionRequest("high1", null, ExecutionPriority.HIGH));
        queue.add(new PriorityExecutionRequest("low1", null, ExecutionPriority.LOW));
        queue.add(new PriorityExecutionRequest("high2", null, ExecutionPriority.HIGH));
        queue.add(new PriorityExecutionRequest("normal2", null, ExecutionPriority.NORMAL));

        // All HIGH requests should come first
        PriorityExecutionRequest first = queue.poll();
        PriorityExecutionRequest second = queue.poll();
        assertEquals(ExecutionPriority.HIGH, first.getPriority());
        assertEquals(ExecutionPriority.HIGH, second.getPriority());

        // Then NORMAL requests
        PriorityExecutionRequest third = queue.poll();
        PriorityExecutionRequest fourth = queue.poll();
        assertEquals(ExecutionPriority.NORMAL, third.getPriority());
        assertEquals(ExecutionPriority.NORMAL, fourth.getPriority());

        // Then LOW request
        assertEquals(ExecutionPriority.LOW, queue.poll().getPriority());
    }

    @Test
    public void testComplete_Success() throws ExecutionException, InterruptedException {
        PriorityExecutionRequest request = new PriorityExecutionRequest("code", null, ExecutionPriority.NORMAL);

        assertFalse(request.getFuture().isDone());

        Python3Result result = new Python3Result(true, 42, null, null);
        request.complete(result);

        assertTrue(request.getFuture().isDone());
        assertEquals(result, request.getFuture().get());
    }

    @Test
    public void testCompleteExceptionally() {
        PriorityExecutionRequest request = new PriorityExecutionRequest("code", null, ExecutionPriority.NORMAL);

        assertFalse(request.getFuture().isDone());

        Exception exception = new RuntimeException("Test error");
        request.completeExceptionally(exception);

        assertTrue(request.getFuture().isDone());
        assertTrue(request.getFuture().isCompletedExceptionally());

        assertThrows(ExecutionException.class, () -> request.getFuture().get());
    }

    @Test
    public void testToString() {
        PriorityExecutionRequest request = new PriorityExecutionRequest(
                "result = x + y + z + very_long_code",
                null,
                ExecutionPriority.HIGH
        );

        String str = request.toString();
        assertTrue(str.contains("HIGH"));
        assertTrue(str.contains("result = x + y + z + very_long"));
        assertFalse(str.contains("very_long_code"), "Code should be truncated");
    }

    @Test
    public void testTimestampUniqueness() throws InterruptedException {
        PriorityExecutionRequest req1 = new PriorityExecutionRequest("code", null, ExecutionPriority.NORMAL);
        Thread.sleep(1);
        PriorityExecutionRequest req2 = new PriorityExecutionRequest("code", null, ExecutionPriority.NORMAL);

        assertNotEquals(req1.getTimestamp(), req2.getTimestamp(),
                "Different requests should have different timestamps");
        assertTrue(req2.getTimestamp() > req1.getTimestamp(),
                "Later request should have later timestamp");
    }

    @Test
    public void testNullVariables() {
        PriorityExecutionRequest request = new PriorityExecutionRequest("code", null, ExecutionPriority.NORMAL);
        assertNull(request.getVariables());
    }

    @Test
    public void testEmptyVariables() {
        Map<String, Object> emptyVars = new HashMap<>();
        PriorityExecutionRequest request = new PriorityExecutionRequest("code", emptyVars, ExecutionPriority.NORMAL);
        assertEquals(emptyVars, request.getVariables());
        assertTrue(request.getVariables().isEmpty());
    }
}
