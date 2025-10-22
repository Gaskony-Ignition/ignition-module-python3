package com.inductiveautomation.ignition.examples.python3.gateway;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Manages a pool of Python 3 processes for efficient execution.
 * Processes are kept alive and reused across multiple script executions.
 */
public class Python3ProcessPool {

    private static final Logger LOGGER = LoggerFactory.getLogger(Python3ProcessPool.class);

    private final String pythonPath;
    private volatile int poolSize;  // Changed to volatile for dynamic resizing (v1.17.2)
    private final BlockingQueue<Python3Executor> availableExecutors;
    private final CopyOnWriteArrayList<Python3Executor> allExecutors;
    private final ScheduledExecutorService healthCheckExecutor;
    private volatile boolean isShutdown = false;
    private final AtomicInteger executorIdCounter = new AtomicInteger(0);

    /**
     * Create a new process pool
     *
     * @param pythonPath Path to Python 3 executable
     * @param poolSize   Number of processes to maintain
     * @throws IOException if processes cannot be started
     */
    public Python3ProcessPool(String pythonPath, int poolSize) throws IOException {
        this.pythonPath = pythonPath;
        this.poolSize = poolSize;
        this.availableExecutors = new LinkedBlockingQueue<>(poolSize);
        this.allExecutors = new CopyOnWriteArrayList<>();

        LOGGER.info("Initializing Python 3 process pool with {} processes", poolSize);

        // Create initial pool
        for (int i = 0; i < poolSize; i++) {
            Python3Executor executor = createExecutor();
            allExecutors.add(executor);
            availableExecutors.offer(executor);
        }

        // Start health check scheduler
        healthCheckExecutor = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "Python3-HealthCheck");
            t.setDaemon(true);
            return t;
        });

        healthCheckExecutor.scheduleAtFixedRate(
                this::performHealthCheck,
                30, // Initial delay
                30, // Period
                TimeUnit.SECONDS
        );

        LOGGER.info("Python 3 process pool initialized successfully");
    }

    /**
     * Create a new executor instance
     */
    private Python3Executor createExecutor() throws IOException {
        int id = executorIdCounter.incrementAndGet();
        LOGGER.debug("Creating Python executor #{}", id);

        try {
            Python3Executor executor = new Python3Executor(pythonPath);
            LOGGER.info("Python executor #{} created successfully", id);
            return executor;
        } catch (IOException e) {
            LOGGER.error("Failed to create Python executor #{}", id, e);
            throw e;
        }
    }

    /**
     * Borrow an executor from the pool
     *
     * @param timeout  Maximum time to wait
     * @param timeUnit Time unit
     * @return An available executor
     * @throws InterruptedException if interrupted while waiting
     * @throws TimeoutException     if no executor becomes available in time
     * @throws IllegalStateException if pool is shutdown
     */
    public Python3Executor borrowExecutor(long timeout, TimeUnit timeUnit)
            throws InterruptedException, TimeoutException {

        if (isShutdown) {
            throw new IllegalStateException("Process pool is shutdown");
        }

        Python3Executor executor = availableExecutors.poll(timeout, timeUnit);

        if (executor == null) {
            throw new TimeoutException("No Python executor available within " + timeout + " " + timeUnit);
        }

        // Double-check executor is healthy
        if (!executor.isHealthy()) {
            LOGGER.warn("Borrowed executor is unhealthy, attempting to replace");
            try {
                replaceExecutor(executor);
                // Try to borrow again (non-blocking)
                executor = availableExecutors.poll();
                if (executor == null) {
                    throw new TimeoutException("No healthy executor available");
                }
            } catch (IOException e) {
                LOGGER.error("Failed to replace unhealthy executor", e);
                throw new TimeoutException("No healthy executor available");
            }
        }

        LOGGER.debug("Executor borrowed, {} available", availableExecutors.size());
        return executor;
    }

    /**
     * Return an executor to the pool
     *
     * @param executor The executor to return
     */
    public void returnExecutor(Python3Executor executor) {
        if (executor == null || isShutdown) {
            return;
        }

        // Check if executor is still healthy
        if (!executor.isHealthy()) {
            LOGGER.warn("Returned executor is unhealthy, will be replaced");
            try {
                replaceExecutor(executor);
            } catch (IOException e) {
                LOGGER.error("Failed to replace unhealthy executor", e);
            }
        } else {
            availableExecutors.offer(executor);
            LOGGER.debug("Executor returned, {} available", availableExecutors.size());
        }
    }

    /**
     * Execute code using a pooled executor
     *
     * @param code      Python code to execute
     * @param variables Variables to pass
     * @return Result
     * @throws Python3Exception if execution fails
     */
    public Python3Result execute(String code, java.util.Map<String, Object> variables) throws Python3Exception {
        return execute(code, variables, "RESTRICTED");
    }

    /**
     * Execute code using a pooled executor with security mode
     *
     * @param code         Python code to execute
     * @param variables    Variables to pass
     * @param securityMode Security mode: "RESTRICTED" or "ADMIN"
     * @return Result
     * @throws Python3Exception if execution fails
     */
    public Python3Result execute(String code, java.util.Map<String, Object> variables, String securityMode) throws Python3Exception {
        Python3Executor executor = null;
        try {
            executor = borrowExecutor(30, TimeUnit.SECONDS);
            return executor.execute(code, variables, securityMode);
        } catch (InterruptedException | TimeoutException e) {
            throw new Python3Exception("Failed to acquire executor: " + e.getMessage(), e);
        } finally {
            if (executor != null) {
                returnExecutor(executor);
            }
        }
    }

    /**
     * Evaluate expression using a pooled executor
     */
    public Python3Result evaluate(String expression, java.util.Map<String, Object> variables) throws Python3Exception {
        return evaluate(expression, variables, "RESTRICTED");
    }

    /**
     * Evaluate expression using a pooled executor with security mode
     *
     * @param expression   Python expression to evaluate
     * @param variables    Variables to pass
     * @param securityMode Security mode: "RESTRICTED" or "ADMIN"
     * @return Result
     * @throws Python3Exception if evaluation fails
     */
    public Python3Result evaluate(String expression, java.util.Map<String, Object> variables, String securityMode) throws Python3Exception {
        Python3Executor executor = null;
        try {
            executor = borrowExecutor(30, TimeUnit.SECONDS);
            return executor.evaluate(expression, variables, securityMode);
        } catch (InterruptedException | TimeoutException e) {
            throw new Python3Exception("Failed to acquire executor: " + e.getMessage(), e);
        } finally {
            if (executor != null) {
                returnExecutor(executor);
            }
        }
    }

    /**
     * Call module function using a pooled executor
     */
    public Python3Result callModule(String moduleName, String functionName,
                                     java.util.List<Object> args,
                                     java.util.Map<String, Object> kwargs) throws Python3Exception {
        return callModule(moduleName, functionName, args, kwargs, "RESTRICTED");
    }

    /**
     * Call module function using a pooled executor with security mode
     *
     * @param moduleName   Module name
     * @param functionName Function name
     * @param args         Arguments
     * @param kwargs       Keyword arguments
     * @param securityMode Security mode: "RESTRICTED" or "ADMIN"
     * @return Result
     * @throws Python3Exception if call fails
     */
    public Python3Result callModule(String moduleName, String functionName,
                                     java.util.List<Object> args,
                                     java.util.Map<String, Object> kwargs,
                                     String securityMode) throws Python3Exception {
        Python3Executor executor = null;
        try {
            executor = borrowExecutor(30, TimeUnit.SECONDS);
            return executor.callModule(moduleName, functionName, args, kwargs, securityMode);
        } catch (InterruptedException | TimeoutException e) {
            throw new Python3Exception("Failed to acquire executor: " + e.getMessage(), e);
        } finally {
            if (executor != null) {
                returnExecutor(executor);
            }
        }
    }

    /**
     * Check Python code syntax using a pooled executor
     *
     * @param code Python code to check
     * @return Result containing list of syntax errors
     * @throws Python3Exception if syntax check fails
     */
    public Python3Result checkSyntax(String code) throws Python3Exception {
        Python3Executor executor = null;
        try {
            executor = borrowExecutor(30, TimeUnit.SECONDS);
            return executor.checkSyntax(code);
        } catch (InterruptedException | TimeoutException e) {
            throw new Python3Exception("Failed to acquire executor: " + e.getMessage(), e);
        } finally {
            if (executor != null) {
                returnExecutor(executor);
            }
        }
    }

    /**
     * Get code completions using a pooled executor
     *
     * @param code   Python code
     * @param line   Line number (1-based)
     * @param column Column number (0-based)
     * @return Result containing list of completions
     * @throws Python3Exception if completions request fails
     */
    public Python3Result getCompletions(String code, int line, int column) throws Python3Exception {
        Python3Executor executor = null;
        try {
            executor = borrowExecutor(30, TimeUnit.SECONDS);
            return executor.getCompletions(code, line, column);
        } catch (InterruptedException | TimeoutException e) {
            throw new Python3Exception("Failed to acquire executor: " + e.getMessage(), e);
        } finally {
            if (executor != null) {
                returnExecutor(executor);
            }
        }
    }

    /**
     * Replace an unhealthy executor with a new one
     */
    private synchronized void replaceExecutor(Python3Executor oldExecutor) throws IOException {
        LOGGER.info("Replacing unhealthy executor");

        // Shutdown old executor
        try {
            oldExecutor.shutdown();
        } catch (Exception e) {
            LOGGER.error("Error shutting down old executor", e);
        }

        // Remove from all executors list
        allExecutors.remove(oldExecutor);

        // Create new executor
        Python3Executor newExecutor = createExecutor();
        allExecutors.add(newExecutor);
        availableExecutors.offer(newExecutor);

        LOGGER.info("Executor replaced successfully");
    }

    /**
     * Perform health check on all executors
     */
    private void performHealthCheck() {
        if (isShutdown) {
            return;
        }

        LOGGER.debug("Performing health check on {} executors", allExecutors.size());

        for (Python3Executor executor : allExecutors) {
            if (!executor.isHealthy()) {
                LOGGER.warn("Executor failed health check, attempting to replace");
                try {
                    replaceExecutor(executor);
                } catch (IOException e) {
                    LOGGER.error("Failed to replace unhealthy executor during health check", e);
                }
            }
        }
    }

    /**
     * Get pool statistics
     */
    public PoolStats getStats() {
        return new PoolStats(
                poolSize,
                availableExecutors.size(),
                poolSize - availableExecutors.size(),
                (int) allExecutors.stream().filter(Python3Executor::isHealthy).count()
        );
    }

    /**
     * Resize the process pool to a new size (1-20).
     * If increasing, new executors are created.
     * If decreasing, excess executors are gracefully shut down.
     *
     * @param newSize the new pool size (1-20)
     * @throws IllegalArgumentException if newSize is out of range
     * @throws IllegalStateException if pool is already shutdown
     *
     * v1.17.2: Added for dynamic pool size adjustment
     */
    public synchronized void resizePool(int newSize) {
        if (newSize < 1 || newSize > 20) {
            throw new IllegalArgumentException("Pool size must be between 1 and 20");
        }

        if (isShutdown) {
            throw new IllegalStateException("Cannot resize shutdown pool");
        }

        int currentSize = poolSize;
        if (newSize == currentSize) {
            LOGGER.info("Pool size already {}, no resize needed", newSize);
            return;
        }

        LOGGER.info("Resizing pool from {} to {}", currentSize, newSize);

        if (newSize > currentSize) {
            // Increase pool size - create new executors
            int toAdd = newSize - currentSize;
            for (int i = 0; i < toAdd; i++) {
                try {
                    Python3Executor executor = createExecutor();
                    allExecutors.add(executor);
                    availableExecutors.offer(executor);
                    LOGGER.info("Added executor {} of {}", i + 1, toAdd);
                } catch (IOException e) {
                    LOGGER.error("Failed to create executor during pool resize", e);
                    // Continue trying to create remaining executors
                }
            }
        } else {
            // Decrease pool size - remove excess executors
            int toRemove = currentSize - newSize;
            for (int i = 0; i < toRemove; i++) {
                // Try to remove from available executors first (not currently in use)
                Python3Executor executor = availableExecutors.poll();
                if (executor != null) {
                    allExecutors.remove(executor);
                    try {
                        executor.shutdown();
                        LOGGER.info("Removed available executor {} of {}", i + 1, toRemove);
                    } catch (Exception e) {
                        LOGGER.error("Error shutting down executor during resize", e);
                    }
                } else {
                    LOGGER.warn("No available executors to remove, {} executors currently in use",
                            currentSize - availableExecutors.size());
                    break;
                }
            }
        }

        poolSize = newSize;
        LOGGER.info("Pool resized to {} (healthy: {}, available: {})",
                newSize, allExecutors.stream().filter(Python3Executor::isHealthy).count(), availableExecutors.size());
    }

    /**
     * Get the current pool size.
     *
     * @return the current pool size
     *
     * v1.17.2: Added for dynamic pool size querying
     */
    public int getPoolSize() {
        return poolSize;
    }

    /**
     * Shutdown the process pool
     */
    public void shutdown() {
        LOGGER.info("Shutting down Python 3 process pool");
        isShutdown = true;

        // Stop health check
        healthCheckExecutor.shutdownNow();

        // Shutdown all executors
        for (Python3Executor executor : allExecutors) {
            try {
                executor.shutdown();
            } catch (Exception e) {
                LOGGER.error("Error shutting down executor", e);
            }
        }

        allExecutors.clear();
        availableExecutors.clear();

        LOGGER.info("Python 3 process pool shutdown complete");
    }

    /**
     * Check if pool is shutdown
     */
    public boolean isShutdown() {
        return isShutdown;
    }

    /**
     * Pool statistics
     */
    public static class PoolStats {
        public final int totalSize;
        public final int available;
        public final int inUse;
        public final int healthy;

        public PoolStats(int totalSize, int available, int inUse, int healthy) {
            this.totalSize = totalSize;
            this.available = available;
            this.inUse = inUse;
            this.healthy = healthy;
        }

        @Override
        public String toString() {
            return String.format("PoolStats{total=%d, available=%d, inUse=%d, healthy=%d}",
                    totalSize, available, inUse, healthy);
        }
    }
}
