package com.inductiveautomation.ignition.examples.python3.gateway;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Tracks health metrics for a Python3Executor.
 *
 * Health scoring includes:
 * - Liveness (is process alive?)
 * - Performance (response time within threshold?)
 * - Memory usage (within limits?)
 * - Error rate (below threshold?)
 *
 * Health score ranges from 0-100:
 * - 100: Perfect health
 * - 75-99: Good health
 * - 50-74: Fair health
 * - 25-49: Poor health
 * - 0-24: Critical health (should be replaced)
 *
 * @since v2.13.0 (Phase 2 Week 3-4: Enhanced Monitoring)
 */
public class ExecutorHealthMetrics {

    private static final Logger LOGGER = LoggerFactory.getLogger(ExecutorHealthMetrics.class);

    // Health thresholds
    private static final long RESPONSE_TIME_THRESHOLD_MS = 5000; // 5 seconds
    private static final long MEMORY_THRESHOLD_MB = 500; // 500 MB
    private static final double ERROR_RATE_THRESHOLD = 0.10; // 10% error rate

    // Metrics
    private long totalExecutions;
    private long successfulExecutions;
    private long failedExecutions;
    private long totalResponseTimeMs;
    private long lastResponseTimeMs;
    private long lastCheckTimestamp;
    private boolean isAlive;
    private long memoryUsageMB;

    // Health score (0-100)
    private int healthScore;

    public ExecutorHealthMetrics() {
        this.totalExecutions = 0;
        this.successfulExecutions = 0;
        this.failedExecutions = 0;
        this.totalResponseTimeMs = 0;
        this.lastResponseTimeMs = 0;
        this.lastCheckTimestamp = System.currentTimeMillis();
        this.isAlive = true;
        this.memoryUsageMB = 0;
        this.healthScore = 100; // Start with perfect health
    }

    /**
     * Record a successful execution.
     */
    public synchronized void recordSuccess(long responseTimeMs) {
        totalExecutions++;
        successfulExecutions++;
        totalResponseTimeMs += responseTimeMs;
        lastResponseTimeMs = responseTimeMs;
        updateHealthScore();
    }

    /**
     * Record a failed execution.
     */
    public synchronized void recordFailure(long responseTimeMs) {
        totalExecutions++;
        failedExecutions++;
        totalResponseTimeMs += responseTimeMs;
        lastResponseTimeMs = responseTimeMs;
        updateHealthScore();
    }

    /**
     * Update liveness status.
     */
    public synchronized void updateLiveness(boolean alive) {
        this.isAlive = alive;
        this.lastCheckTimestamp = System.currentTimeMillis();
        updateHealthScore();
    }

    /**
     * Update memory usage.
     */
    public synchronized void updateMemoryUsage(long memoryMB) {
        this.memoryUsageMB = memoryMB;
        updateHealthScore();
    }

    /**
     * Calculate health score (0-100) based on multiple factors.
     */
    private void updateHealthScore() {
        int score = 100;

        // Factor 1: Liveness (critical - 40 points)
        if (!isAlive) {
            score -= 40;
        }

        // Factor 2: Error rate (30 points)
        if (totalExecutions > 0) {
            double errorRate = (double) failedExecutions / totalExecutions;
            if (errorRate > ERROR_RATE_THRESHOLD) {
                // Penalize based on how much over threshold
                int penalty = (int) Math.min(30, (errorRate - ERROR_RATE_THRESHOLD) * 100);
                score -= penalty;
            }
        }

        // Factor 3: Performance (20 points)
        if (lastResponseTimeMs > RESPONSE_TIME_THRESHOLD_MS) {
            // Penalize based on how much slower
            double slowFactor = (double) lastResponseTimeMs / RESPONSE_TIME_THRESHOLD_MS;
            int penalty = (int) Math.min(20, (slowFactor - 1.0) * 10);
            score -= penalty;
        }

        // Factor 4: Memory usage (10 points)
        if (memoryUsageMB > MEMORY_THRESHOLD_MB) {
            // Penalize based on how much over threshold
            double memoryFactor = (double) memoryUsageMB / MEMORY_THRESHOLD_MB;
            int penalty = (int) Math.min(10, (memoryFactor - 1.0) * 10);
            score -= penalty;
        }

        // Ensure score stays in valid range
        this.healthScore = Math.max(0, Math.min(100, score));

        // Log health status changes
        if (this.healthScore < 50 && totalExecutions > 0) {
            LOGGER.warn("Executor health degraded: score={}, errorRate={:.2f}%, avgResponseTime={}ms, memory={}MB",
                healthScore,
                getErrorRate() * 100,
                getAverageResponseTimeMs(),
                memoryUsageMB);
        }
    }

    /**
     * Get health status as a string.
     */
    public synchronized String getHealthStatus() {
        if (healthScore >= 75) return "HEALTHY";
        if (healthScore >= 50) return "FAIR";
        if (healthScore >= 25) return "POOR";
        return "CRITICAL";
    }

    /**
     * Check if executor needs replacement.
     */
    public synchronized boolean needsReplacement() {
        return healthScore < 25 || !isAlive;
    }

    // Getters

    public synchronized int getHealthScore() {
        return healthScore;
    }

    public synchronized long getTotalExecutions() {
        return totalExecutions;
    }

    public synchronized long getSuccessfulExecutions() {
        return successfulExecutions;
    }

    public synchronized long getFailedExecutions() {
        return failedExecutions;
    }

    public synchronized double getErrorRate() {
        return totalExecutions == 0 ? 0.0 : (double) failedExecutions / totalExecutions;
    }

    public synchronized long getAverageResponseTimeMs() {
        return totalExecutions == 0 ? 0 : totalResponseTimeMs / totalExecutions;
    }

    public synchronized long getLastResponseTimeMs() {
        return lastResponseTimeMs;
    }

    public synchronized boolean isAlive() {
        return isAlive;
    }

    public synchronized long getMemoryUsageMB() {
        return memoryUsageMB;
    }

    public synchronized long getLastCheckTimestamp() {
        return lastCheckTimestamp;
    }

    /**
     * Get metrics summary as formatted string.
     */
    @Override
    public synchronized String toString() {
        return String.format(
            "ExecutorHealthMetrics{score=%d, status=%s, executions=%d, errors=%d, errorRate=%.2f%%, avgResponseTime=%dms, memory=%dMB, alive=%s}",
            healthScore,
            getHealthStatus(),
            totalExecutions,
            failedExecutions,
            getErrorRate() * 100,
            getAverageResponseTimeMs(),
            memoryUsageMB,
            isAlive
        );
    }
}
