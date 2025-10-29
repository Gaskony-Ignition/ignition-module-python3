package com.inductiveautomation.ignition.examples.python3.gateway;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for Python3ProcessPool.
 *
 * These tests verify the process pool's ability to:
 * - Initialize and manage multiple Python executors
 * - Handle concurrent borrowing and returning
 * - Recover from executor failures
 * - Properly shut down and clean up resources
 *
 * @since v2.12.0 (Phase 2 Week 1-2: Testing Infrastructure)
 */
public class Python3ProcessPoolTest {

    private static final Logger LOGGER = LoggerFactory.getLogger(Python3ProcessPoolTest.class);
    private static final String TEST_PYTHON_PATH = "python3";
    private static final int SMALL_POOL_SIZE = 2;
    private static final int MEDIUM_POOL_SIZE = 3;

    private Python3ProcessPool pool;

    @BeforeEach
    public void setUp() {
        LOGGER.info("Setting up Python3ProcessPoolTest");
    }

    @AfterEach
    public void tearDown() {
        if (pool != null) {
            try {
                pool.shutdown();
            } catch (Exception e) {
                LOGGER.warn("Error during pool shutdown in tearDown", e);
            }
        }
    }

    /**
     * Test 1: Pool initialization creates correct number of processes
     * and all executors are healthy after initialization.
     */
    @Test
    public void testPoolInitialization() throws Exception {
        LOGGER.info("Test: Pool initialization");

        pool = new Python3ProcessPool(TEST_PYTHON_PATH, MEDIUM_POOL_SIZE);

        // Verify pool size
        PoolStats stats = pool.getPoolStats();
        assertNotNull("Pool stats should not be null", stats);
        assertEquals("Pool size should match configuration", MEDIUM_POOL_SIZE, stats.getPoolSize());
        assertEquals("All executors should be available initially", MEDIUM_POOL_SIZE, stats.getAvailableExecutors());
        assertEquals("No executors should be borrowed initially", 0, stats.getBorrowedExecutors());

        // Verify all executors are healthy by borrowing each one
        List<Python3Executor> executors = new ArrayList<>();
        for (int i = 0; i < MEDIUM_POOL_SIZE; i++) {
            Python3Executor executor = pool.borrowExecutor(5, TimeUnit.SECONDS);
            assertNotNull("Should be able to borrow executor " + i, executor);
            assertTrue("Executor " + i + " should be alive", executor.isAlive());
            executors.add(executor);
        }

        // Return all executors
        for (Python3Executor executor : executors) {
            pool.returnExecutor(executor);
        }

        LOGGER.info("✓ Pool initialization test passed");
    }

    /**
     * Test 2: Borrow and return executor lifecycle.
     * Verifies that borrowing decreases available count and returning increases it.
     */
    @Test
    public void testBorrowAndReturnExecutor() throws Exception {
        LOGGER.info("Test: Borrow and return executor");

        pool = new Python3ProcessPool(TEST_PYTHON_PATH, SMALL_POOL_SIZE);

        // Initial state
        PoolStats stats = pool.getPoolStats();
        assertEquals("Initial available executors", SMALL_POOL_SIZE, stats.getAvailableExecutors());
        assertEquals("Initial borrowed executors", 0, stats.getBorrowedExecutors());

        // Borrow an executor
        Python3Executor executor = pool.borrowExecutor(5, TimeUnit.SECONDS);
        assertNotNull("Should successfully borrow executor", executor);

        // Verify pool state changed
        stats = pool.getPoolStats();
        assertEquals("Available executors should decrease", SMALL_POOL_SIZE - 1, stats.getAvailableExecutors());
        assertEquals("Borrowed executors should increase", 1, stats.getBorrowedExecutors());

        // Verify executor works
        assertTrue("Borrowed executor should be alive", executor.isAlive());

        // Return executor
        pool.returnExecutor(executor);

        // Verify pool state restored
        stats = pool.getPoolStats();
        assertEquals("Available executors should be restored", SMALL_POOL_SIZE, stats.getAvailableExecutors());
        assertEquals("Borrowed executors should be zero", 0, stats.getBorrowedExecutors());

        // Verify we can borrow the same executor again
        Python3Executor executor2 = pool.borrowExecutor(5, TimeUnit.SECONDS);
        assertNotNull("Should be able to borrow executor again", executor2);
        assertTrue("Re-borrowed executor should be alive", executor2.isAlive());

        pool.returnExecutor(executor2);

        LOGGER.info("✓ Borrow and return test passed");
    }

    /**
     * Test 3: Concurrent borrowing with proper queuing.
     * Spawns multiple threads trying to borrow from a small pool.
     */
    @Test
    public void testConcurrentBorrowing() throws Exception {
        LOGGER.info("Test: Concurrent borrowing");

        pool = new Python3ProcessPool(TEST_PYTHON_PATH, MEDIUM_POOL_SIZE);

        final int THREAD_COUNT = 10;
        final CountDownLatch startLatch = new CountDownLatch(1);
        final CountDownLatch doneLatch = new CountDownLatch(THREAD_COUNT);
        final List<Exception> exceptions = new CopyOnWriteArrayList<>();
        final List<String> successfulBorrows = new CopyOnWriteArrayList<>();

        // Spawn threads that try to borrow executors
        for (int i = 0; i < THREAD_COUNT; i++) {
            final int threadId = i;
            new Thread(() -> {
                try {
                    startLatch.await(); // Wait for all threads to be ready

                    Python3Executor executor = pool.borrowExecutor(10, TimeUnit.SECONDS);
                    successfulBorrows.add("Thread-" + threadId);

                    // Hold executor briefly
                    Thread.sleep(50);

                    pool.returnExecutor(executor);
                } catch (Exception e) {
                    exceptions.add(e);
                } finally {
                    doneLatch.countDown();
                }
            }).start();
        }

        // Start all threads simultaneously
        startLatch.countDown();

        // Wait for all threads to complete (with timeout)
        boolean completed = doneLatch.await(30, TimeUnit.SECONDS);
        assertTrue("All threads should complete within timeout", completed);

        // Verify no exceptions occurred
        if (!exceptions.isEmpty()) {
            LOGGER.error("Exceptions during concurrent borrowing:");
            exceptions.forEach(e -> LOGGER.error("  - " + e.getMessage(), e));
        }
        assertTrue("No exceptions should occur during concurrent borrowing", exceptions.isEmpty());

        // Verify all threads successfully borrowed an executor
        assertEquals("All threads should successfully borrow", THREAD_COUNT, successfulBorrows.size());

        // Verify final pool state
        PoolStats stats = pool.getPoolStats();
        assertEquals("All executors should be returned", MEDIUM_POOL_SIZE, stats.getAvailableExecutors());
        assertEquals("No executors should be borrowed", 0, stats.getBorrowedExecutors());

        LOGGER.info("✓ Concurrent borrowing test passed");
    }

    /**
     * Test 4: Borrow timeout when pool is exhausted.
     * Verifies that borrowing times out when no executors are available.
     */
    @Test
    public void testBorrowTimeout() throws Exception {
        LOGGER.info("Test: Borrow timeout");

        pool = new Python3ProcessPool(TEST_PYTHON_PATH, 1); // Single executor pool

        // Borrow the only executor
        Python3Executor executor1 = pool.borrowExecutor(5, TimeUnit.SECONDS);
        assertNotNull("Should successfully borrow first executor", executor1);

        // Try to borrow another with short timeout - should timeout
        long startTime = System.currentTimeMillis();
        try {
            pool.borrowExecutor(1, TimeUnit.SECONDS);
            fail("Should have thrown TimeoutException");
        } catch (TimeoutException e) {
            long duration = System.currentTimeMillis() - startTime;
            assertTrue("Timeout should occur around 1 second", duration >= 900 && duration <= 1500);
            LOGGER.info("Expected timeout occurred after {}ms", duration);
        }

        // Return first executor
        pool.returnExecutor(executor1);

        // Now borrowing should succeed immediately
        Python3Executor executor2 = pool.borrowExecutor(5, TimeUnit.SECONDS);
        assertNotNull("Should successfully borrow after return", executor2);

        pool.returnExecutor(executor2);

        LOGGER.info("✓ Borrow timeout test passed");
    }

    /**
     * Test 5: Pool shutdown terminates all processes.
     * Verifies that shutdown cleanly terminates all executors.
     */
    @Test
    public void testPoolShutdown() throws Exception {
        LOGGER.info("Test: Pool shutdown");

        pool = new Python3ProcessPool(TEST_PYTHON_PATH, MEDIUM_POOL_SIZE);

        // Borrow and execute some tasks to ensure executors are active
        List<Python3Executor> executors = new ArrayList<>();
        for (int i = 0; i < MEDIUM_POOL_SIZE; i++) {
            Python3Executor executor = pool.borrowExecutor(5, TimeUnit.SECONDS);
            // Execute a simple task to ensure subprocess is active
            executor.execute("x = 1 + 1", new java.util.HashMap<>());
            executors.add(executor);
        }

        // Return all executors
        for (Python3Executor executor : executors) {
            pool.returnExecutor(executor);
        }

        // Shutdown pool
        pool.shutdown();

        // Verify all executors are terminated
        for (Python3Executor executor : executors) {
            assertFalse("Executor should be terminated after shutdown", executor.isAlive());
        }

        // Verify we can't borrow after shutdown
        try {
            pool.borrowExecutor(1, TimeUnit.SECONDS);
            fail("Should not be able to borrow after shutdown");
        } catch (IllegalStateException e) {
            LOGGER.info("Expected exception after shutdown: {}", e.getMessage());
        } catch (Exception e) {
            // Also acceptable - pool is shut down
            LOGGER.info("Pool correctly prevented borrowing after shutdown");
        }

        pool = null; // Don't double-shutdown in tearDown

        LOGGER.info("✓ Pool shutdown test passed");
    }

    /**
     * Test 6: Executor statistics tracking.
     * Verifies that pool correctly tracks total executions and errors.
     */
    @Test
    public void testExecutorStatistics() throws Exception {
        LOGGER.info("Test: Executor statistics");

        pool = new Python3ProcessPool(TEST_PYTHON_PATH, SMALL_POOL_SIZE);

        // Initial stats
        PoolStats initialStats = pool.getPoolStats();
        long initialTotal = initialStats.getTotalExecutions();
        long initialErrors = initialStats.getTotalErrors();

        // Borrow executor and execute successful code
        Python3Executor executor = pool.borrowExecutor(5, TimeUnit.SECONDS);
        executor.execute("result = 2 + 2", new java.util.HashMap<>());
        pool.returnExecutor(executor);

        // Check stats increased
        PoolStats afterSuccess = pool.getPoolStats();
        assertEquals("Total executions should increase by 1", initialTotal + 1, afterSuccess.getTotalExecutions());
        assertEquals("Total errors should not increase", initialErrors, afterSuccess.getTotalErrors());

        // Execute code that causes an error
        executor = pool.borrowExecutor(5, TimeUnit.SECONDS);
        try {
            executor.execute("x = undefined_variable", new java.util.HashMap<>());
        } catch (Exception e) {
            // Expected - undefined variable
        }
        pool.returnExecutor(executor);

        // Check error stats increased
        PoolStats afterError = pool.getPoolStats();
        assertEquals("Total executions should increase by 2", initialTotal + 2, afterError.getTotalExecutions());
        assertTrue("Total errors should increase", afterError.getTotalErrors() > initialErrors);

        LOGGER.info("✓ Executor statistics test passed");
    }

    /**
     * Test 7: Pool health check and executor replacement.
     * Verifies that unhealthy executors are detected and replaced.
     *
     * Note: This test may be skipped if health checking is not fully implemented yet.
     */
    @Test
    public void testHealthCheckAndRecovery() throws Exception {
        LOGGER.info("Test: Health check and recovery (may be limited by implementation)");

        pool = new Python3ProcessPool(TEST_PYTHON_PATH, SMALL_POOL_SIZE);

        // Get initial pool state
        PoolStats initialStats = pool.getPoolStats();
        int initialPoolSize = initialStats.getPoolSize();

        // Borrow an executor
        Python3Executor executor = pool.borrowExecutor(5, TimeUnit.SECONDS);

        // Try to kill the executor's subprocess to simulate failure
        // (This may not work depending on implementation)
        try {
            executor.forceShutdown(); // If this method exists
        } catch (Exception e) {
            LOGGER.warn("Could not force shutdown executor: {}", e.getMessage());
        }

        // Return the potentially unhealthy executor
        pool.returnExecutor(executor);

        // Verify pool still functions
        Python3Executor newExecutor = pool.borrowExecutor(5, TimeUnit.SECONDS);
        assertNotNull("Should still be able to borrow executor", newExecutor);
        assertTrue("Borrowed executor should be alive", newExecutor.isAlive());

        pool.returnExecutor(newExecutor);

        // Verify pool maintained correct size
        PoolStats finalStats = pool.getPoolStats();
        assertEquals("Pool size should remain constant", initialPoolSize, finalStats.getPoolSize());

        LOGGER.info("✓ Health check test passed (basic verification)");
    }

    /**
     * Test 8: Multiple borrow and return cycles.
     * Verifies that executors can be reused many times without issues.
     */
    @Test
    public void testMultipleBorrowReturnCycles() throws Exception {
        LOGGER.info("Test: Multiple borrow/return cycles");

        pool = new Python3ProcessPool(TEST_PYTHON_PATH, SMALL_POOL_SIZE);

        final int CYCLES = 20;

        for (int i = 0; i < CYCLES; i++) {
            Python3Executor executor = pool.borrowExecutor(5, TimeUnit.SECONDS);
            assertNotNull("Cycle " + i + ": Should borrow executor", executor);
            assertTrue("Cycle " + i + ": Executor should be alive", executor.isAlive());

            // Execute simple code
            executor.execute("x = " + i, new java.util.HashMap<>());

            pool.returnExecutor(executor);
        }

        // Verify pool is still healthy
        PoolStats finalStats = pool.getPoolStats();
        assertEquals("All executors should be available", SMALL_POOL_SIZE, finalStats.getAvailableExecutors());
        assertTrue("Total executions should be at least CYCLES", finalStats.getTotalExecutions() >= CYCLES);

        LOGGER.info("✓ Multiple cycles test passed ({} cycles)", CYCLES);
    }
}
