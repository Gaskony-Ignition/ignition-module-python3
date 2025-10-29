package com.inductiveautomation.ignition.examples.python3.gateway;

import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * Priority Execution Request - Wrapper for Python execution with priority support.
 *
 * <p>Used by priority queue to order execution requests by priority.
 * Implements Comparable for natural ordering in PriorityBlockingQueue.
 *
 * @since v2.16.0 (Phase 3 Week 3-4: Performance Optimization)
 */
public class PriorityExecutionRequest implements Comparable<PriorityExecutionRequest> {

    private final String code;
    private final Map<String, Object> variables;
    private final ExecutionPriority priority;
    private final long timestamp; // For FIFO ordering within same priority
    private final CompletableFuture<Python3Result> future;

    /**
     * Create a priority execution request.
     *
     * @param code      Python code to execute
     * @param variables Variables map
     * @param priority  Execution priority
     */
    public PriorityExecutionRequest(String code, Map<String, Object> variables, ExecutionPriority priority) {
        this.code = code;
        this.variables = variables;
        this.priority = priority != null ? priority : ExecutionPriority.NORMAL;
        this.timestamp = System.nanoTime(); // For FIFO within same priority
        this.future = new CompletableFuture<>();
    }

    /**
     * Get Python code.
     */
    public String getCode() {
        return code;
    }

    /**
     * Get variables map.
     */
    public Map<String, Object> getVariables() {
        return variables;
    }

    /**
     * Get execution priority.
     */
    public ExecutionPriority getPriority() {
        return priority;
    }

    /**
     * Get request timestamp.
     */
    public long getTimestamp() {
        return timestamp;
    }

    /**
     * Get future for async result.
     */
    public CompletableFuture<Python3Result> getFuture() {
        return future;
    }

    /**
     * Complete the request with result.
     *
     * @param result Execution result
     */
    public void complete(Python3Result result) {
        future.complete(result);
    }

    /**
     * Complete the request exceptionally.
     *
     * @param throwable Exception
     */
    public void completeExceptionally(Throwable throwable) {
        future.completeExceptionally(throwable);
    }

    /**
     * Compare by priority first, then by timestamp (FIFO within same priority).
     * Lower priority value = higher priority (executed first).
     */
    @Override
    public int compareTo(PriorityExecutionRequest other) {
        // Compare by priority first
        int priorityComparison = Integer.compare(this.priority.getValue(), other.priority.getValue());
        if (priorityComparison != 0) {
            return priorityComparison;
        }

        // Same priority - use FIFO (earliest timestamp first)
        return Long.compare(this.timestamp, other.timestamp);
    }

    @Override
    public String toString() {
        return String.format("PriorityExecutionRequest{priority=%s, timestamp=%d, code=%s...}",
                priority, timestamp, code.substring(0, Math.min(30, code.length())));
    }
}
