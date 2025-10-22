package com.inductiveautomation.ignition.examples.python3.gateway;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

/**
 * Collects performance metrics for Python3 script executions.
 * Tracks execution counts, errors, timing, Gateway impact, per-script metrics, and historical data.
 *
 * NEW in v1.16.0:
 * - Per-script performance metrics
 * - Historical metric snapshots (circular buffer)
 * - Health alerts when thresholds exceeded
 */
public class Python3MetricsCollector {

    private static final Logger LOGGER = LoggerFactory.getLogger(Python3MetricsCollector.class);

    // Historical tracking configuration
    private static final int MAX_HISTORY_SNAPSHOTS = 100;  // Keep last 100 snapshots
    private static final long SNAPSHOT_INTERVAL_MS = 60000;  // 1 minute between snapshots

    // Execution counters
    private final AtomicLong totalExecutions = new AtomicLong(0);
    private final AtomicLong successfulExecutions = new AtomicLong(0);
    private final AtomicLong failedExecutions = new AtomicLong(0);

    // Timing metrics (milliseconds)
    private final AtomicLong totalExecutionTime = new AtomicLong(0);
    private final AtomicLong minExecutionTime = new AtomicLong(Long.MAX_VALUE);
    private final AtomicLong maxExecutionTime = new AtomicLong(0);

    // Pool metrics
    private final AtomicInteger currentPoolSize = new AtomicInteger(0);
    private final AtomicInteger activeExecutions = new AtomicInteger(0);
    private final AtomicLong poolWaitTimeTotal = new AtomicLong(0);
    private final AtomicInteger poolWaitCount = new AtomicInteger(0);

    // Error tracking
    private final Map<String, AtomicInteger> errorCounts = new ConcurrentHashMap<>();

    // Per-script metrics (NEW in v1.16.0)
    private final Map<String, ScriptMetrics> scriptMetrics = new ConcurrentHashMap<>();

    // Historical metric snapshots (NEW in v1.16.0)
    private final List<MetricSnapshot> metricHistory = Collections.synchronizedList(new LinkedList<>());
    private long lastSnapshotTime = System.currentTimeMillis();

    // Health alerts (NEW in v1.16.0)
    private final List<HealthAlert> activeAlerts = Collections.synchronizedList(new ArrayList<>());

    // Start time for uptime calculation
    private final long startTime = System.currentTimeMillis();

    /**
     * Record a successful execution
     */
    public void recordExecution(long executionTimeMs) {
        recordExecution(executionTimeMs, null);
    }

    /**
     * Record a successful execution with script identifier (NEW in v1.16.0)
     *
     * @param executionTimeMs Execution time in milliseconds
     * @param scriptIdentifier Script name or hash (null for anonymous scripts)
     */
    public void recordExecution(long executionTimeMs, String scriptIdentifier) {
        totalExecutions.incrementAndGet();
        successfulExecutions.incrementAndGet();
        totalExecutionTime.addAndGet(executionTimeMs);

        // Update min/max
        updateMin(minExecutionTime, executionTimeMs);
        updateMax(maxExecutionTime, executionTimeMs);

        // Per-script tracking
        if (scriptIdentifier != null && !scriptIdentifier.isEmpty()) {
            scriptMetrics.computeIfAbsent(scriptIdentifier, k -> new ScriptMetrics(scriptIdentifier))
                    .recordExecution(executionTimeMs);
        }

        // Check if snapshot needed
        checkAndCreateSnapshot();

        // Check health alerts
        checkHealthAlerts();

        LOGGER.debug("Recorded execution: {}ms (total: {}, script: {})",
                executionTimeMs, totalExecutions.get(), scriptIdentifier != null ? scriptIdentifier : "anonymous");
    }

    /**
     * Record a failed execution
     */
    public void recordFailure(String errorType, long executionTimeMs) {
        recordFailure(errorType, executionTimeMs, null);
    }

    /**
     * Record a failed execution with script identifier (NEW in v1.16.0)
     *
     * @param errorType Error type/message
     * @param executionTimeMs Execution time before failure
     * @param scriptIdentifier Script name or hash (null for anonymous scripts)
     */
    public void recordFailure(String errorType, long executionTimeMs, String scriptIdentifier) {
        totalExecutions.incrementAndGet();
        failedExecutions.incrementAndGet();
        totalExecutionTime.addAndGet(executionTimeMs);

        // Track error types
        errorCounts.computeIfAbsent(errorType, k -> new AtomicInteger(0)).incrementAndGet();

        // Per-script tracking
        if (scriptIdentifier != null && !scriptIdentifier.isEmpty()) {
            scriptMetrics.computeIfAbsent(scriptIdentifier, k -> new ScriptMetrics(scriptIdentifier))
                    .recordFailure(errorType, executionTimeMs);
        }

        // Check health alerts
        checkHealthAlerts();

        LOGGER.debug("Recorded failure: {} ({}ms, script: {})",
                errorType, executionTimeMs, scriptIdentifier != null ? scriptIdentifier : "anonymous");
    }

    /**
     * Record pool wait time
     */
    public void recordPoolWait(long waitTimeMs) {
        poolWaitTimeTotal.addAndGet(waitTimeMs);
        poolWaitCount.incrementAndGet();
    }

    /**
     * Update pool size
     */
    public void setPoolSize(int size) {
        currentPoolSize.set(size);
    }

    /**
     * Increment active execution count
     */
    public void incrementActiveExecutions() {
        activeExecutions.incrementAndGet();
    }

    /**
     * Decrement active execution count
     */
    public void decrementActiveExecutions() {
        activeExecutions.decrementAndGet();
    }

    /**
     * Get comprehensive metrics
     */
    public Map<String, Object> getMetrics() {
        Map<String, Object> metrics = new HashMap<>();

        // Execution metrics
        long total = totalExecutions.get();
        long successful = successfulExecutions.get();
        long failed = failedExecutions.get();

        metrics.put("total_executions", total);
        metrics.put("successful_executions", successful);
        metrics.put("failed_executions", failed);
        metrics.put("success_rate", total > 0 ? (double) successful / total * 100.0 : 100.0);

        // Timing metrics
        long totalTime = totalExecutionTime.get();
        metrics.put("total_execution_time_ms", totalTime);
        metrics.put("average_execution_time_ms", total > 0 ? totalTime / total : 0);
        metrics.put("min_execution_time_ms", minExecutionTime.get() == Long.MAX_VALUE ? 0 : minExecutionTime.get());
        metrics.put("max_execution_time_ms", maxExecutionTime.get());

        // Pool metrics
        metrics.put("pool_size", currentPoolSize.get());
        metrics.put("active_executions", activeExecutions.get());
        metrics.put("pool_utilization", currentPoolSize.get() > 0 ?
            (double) activeExecutions.get() / currentPoolSize.get() * 100.0 : 0.0);

        int waitCount = poolWaitCount.get();
        metrics.put("pool_wait_count", waitCount);
        metrics.put("average_pool_wait_ms", waitCount > 0 ? poolWaitTimeTotal.get() / waitCount : 0);

        // Error metrics
        Map<String, Integer> errors = new HashMap<>();
        errorCounts.forEach((key, value) -> errors.put(key, value.get()));
        metrics.put("error_counts", errors);

        // Uptime
        long uptimeMs = System.currentTimeMillis() - startTime;
        metrics.put("uptime_ms", uptimeMs);
        metrics.put("uptime_hours", uptimeMs / (1000.0 * 60 * 60));

        // Health score (0-100, based on success rate and pool availability)
        double successRate = total > 0 ? (double) successful / total * 100.0 : 100.0;
        double poolAvailability = currentPoolSize.get() > 0 ?
            (double) (currentPoolSize.get() - activeExecutions.get()) / currentPoolSize.get() * 100.0 : 100.0;
        double healthScore = (successRate * 0.7) + (poolAvailability * 0.3);  // Weighted average
        metrics.put("health_score", Math.round(healthScore));

        return metrics;
    }

    /**
     * Get Gateway impact assessment
     */
    public Map<String, Object> getGatewayImpact() {
        Map<String, Object> impact = new HashMap<>();

        long total = totalExecutions.get();
        long totalTime = totalExecutionTime.get();
        long uptimeMs = System.currentTimeMillis() - startTime;

        // Execution rate (executions per minute)
        double executionRate = uptimeMs > 0 ? (double) total / (uptimeMs / 60000.0) : 0.0;
        impact.put("executions_per_minute", Math.round(executionRate * 100.0) / 100.0);

        // v2.5.21: CPU usage percentage - calculate as time spent in Python vs uptime
        double cpuUsagePercent = 0.0;
        if (uptimeMs > 0) {
            // Calculate what % of time was spent executing Python code
            cpuUsagePercent = ((double) totalTime / (double) uptimeMs) * 100.0;
            cpuUsagePercent = Math.min(100.0, cpuUsagePercent);  // Cap at 100%
        }
        impact.put("cpu_usage_percent", Math.round(cpuUsagePercent * 100.0) / 100.0);
        impact.put("cpuUsagePercent", Math.round(cpuUsagePercent * 100.0) / 100.0);  // v2.5.21: Camel case for JSON parsing

        // Average CPU time consumed (kept for compatibility)
        long avgCpuTime = total > 0 ? totalTime / total : 0;
        impact.put("average_cpu_time_ms", avgCpuTime);
        impact.put("averageCpuTimeMs", (double) avgCpuTime);  // v2.5.19: Camel case for JSON parsing

        // v2.5.19: Memory usage - get current JVM memory used by Gateway
        Runtime runtime = Runtime.getRuntime();
        long usedMemory = runtime.totalMemory() - runtime.freeMemory();
        double usedMemoryMb = usedMemory / (1024.0 * 1024.0);
        impact.put("memory_usage_mb", Math.round(usedMemoryMb * 100.0) / 100.0);
        impact.put("memoryUsageMb", Math.round(usedMemoryMb * 100.0) / 100.0);  // v2.5.19: Camel case for JSON parsing

        // Pool contention (wait time indicates resource pressure)
        int waitCount = poolWaitCount.get();
        impact.put("pool_contention_events", waitCount);
        impact.put("average_wait_time_ms", waitCount > 0 ? poolWaitTimeTotal.get() / waitCount : 0);

        // Resource utilization
        double poolUtil = currentPoolSize.get() > 0 ?
            (double) activeExecutions.get() / currentPoolSize.get() * 100.0 : 0.0;
        impact.put("pool_utilization_percent", Math.round(poolUtil * 100.0) / 100.0);

        // Impact level assessment
        String impactLevel;
        if (poolUtil > 80 || executionRate > 50) {
            impactLevel = "HIGH";
        } else if (poolUtil > 50 || executionRate > 20) {
            impactLevel = "MEDIUM";
        } else {
            impactLevel = "LOW";
        }
        impact.put("impact_level", impactLevel);
        impact.put("impactLevel", impactLevel);  // v2.5.19: Camel case for JSON parsing

        // v2.5.19: Health score calculation (0-100)
        int healthScore = 100;
        if (poolUtil > 85) {
            healthScore -= 30;
        } else if (poolUtil > 60) {
            healthScore -= 15;
        }
        if (executionRate > 50) {
            healthScore -= 20;
        } else if (executionRate > 20) {
            healthScore -= 10;
        }
        long failedCount = failedExecutions.get();
        double successRate = total > 0 ? (double) (total - failedCount) / total * 100.0 : 100.0;
        if (successRate < 90) {
            healthScore -= 20;
        } else if (successRate < 95) {
            healthScore -= 10;
        }
        impact.put("health_score", Math.max(0, healthScore));
        impact.put("healthScore", Math.max(0, healthScore));  // v2.5.19: Camel case for JSON parsing

        return impact;
    }

    /**
     * Reset all metrics
     */
    public void reset() {
        totalExecutions.set(0);
        successfulExecutions.set(0);
        failedExecutions.set(0);
        totalExecutionTime.set(0);
        minExecutionTime.set(Long.MAX_VALUE);
        maxExecutionTime.set(0);
        poolWaitTimeTotal.set(0);
        poolWaitCount.set(0);
        errorCounts.clear();

        LOGGER.info("Metrics reset");
    }

    /**
     * Atomic min update
     */
    private void updateMin(AtomicLong atomic, long value) {
        long current;
        do {
            current = atomic.get();
            if (value >= current) {
                break;
            }
        } while (!atomic.compareAndSet(current, value));
    }

    /**
     * Atomic max update
     */
    private void updateMax(AtomicLong atomic, long value) {
        long current;
        do {
            current = atomic.get();
            if (value <= current) {
                break;
            }
        } while (!atomic.compareAndSet(current, value));
    }

    /**
     * Get per-script performance metrics (NEW in v1.16.0)
     *
     * @return List of script metrics, sorted by total execution count
     */
    public List<Map<String, Object>> getScriptMetrics() {
        return scriptMetrics.values().stream()
                .sorted((a, b) -> Long.compare(b.getTotalExecutions(), a.getTotalExecutions()))
                .limit(50)  // Top 50 scripts
                .map(ScriptMetrics::toMap)
                .collect(Collectors.toList());
    }

    /**
     * Get historical metric snapshots (NEW in v1.16.0)
     *
     * @return List of metric snapshots (most recent first)
     */
    public List<Map<String, Object>> getHistoricalMetrics() {
        synchronized (metricHistory) {
            return metricHistory.stream()
                    .map(MetricSnapshot::toMap)
                    .collect(Collectors.toList());
        }
    }

    /**
     * Get active health alerts (NEW in v1.16.0)
     *
     * @return List of active alerts
     */
    public List<Map<String, Object>> getHealthAlerts() {
        synchronized (activeAlerts) {
            return activeAlerts.stream()
                    .map(HealthAlert::toMap)
                    .collect(Collectors.toList());
        }
    }

    /**
     * Check if periodic snapshot is needed and create it (NEW in v1.16.0)
     */
    private void checkAndCreateSnapshot() {
        long now = System.currentTimeMillis();
        if (now - lastSnapshotTime >= SNAPSHOT_INTERVAL_MS) {
            synchronized (metricHistory) {
                // Create snapshot
                MetricSnapshot snapshot = new MetricSnapshot(
                        now,
                        totalExecutions.get(),
                        successfulExecutions.get(),
                        failedExecutions.get(),
                        totalExecutionTime.get() / Math.max(1, totalExecutions.get()),
                        activeExecutions.get(),
                        currentPoolSize.get()
                );

                metricHistory.add(0, snapshot);  // Add to front (most recent)

                // Remove old snapshots (keep only MAX_HISTORY_SNAPSHOTS)
                while (metricHistory.size() > MAX_HISTORY_SNAPSHOTS) {
                    metricHistory.remove(metricHistory.size() - 1);
                }

                lastSnapshotTime = now;
                LOGGER.debug("Created metric snapshot: total_executions={}, pool_utilization={}%",
                        totalExecutions.get(), snapshot.poolUtilization);
            }
        }
    }

    /**
     * Check health thresholds and generate alerts (NEW in v1.16.0)
     */
    private void checkHealthAlerts() {
        long now = System.currentTimeMillis();

        // Clear stale alerts (older than 5 minutes)
        synchronized (activeAlerts) {
            activeAlerts.removeIf(alert -> now - alert.timestamp > 300000);
        }

        // Check pool utilization alert
        double poolUtil = currentPoolSize.get() > 0 ?
                (double) activeExecutions.get() / currentPoolSize.get() * 100.0 : 0.0;

        if (poolUtil > 90) {
            addAlert("POOL_UTILIZATION_CRITICAL", "Pool utilization is critically high (" +
                    Math.round(poolUtil) + "%). Consider increasing pool size.", "CRITICAL");
        } else if (poolUtil > 70) {
            addAlert("POOL_UTILIZATION_HIGH", "Pool utilization is high (" +
                    Math.round(poolUtil) + "%). Monitor for performance impact.", "WARNING");
        }

        // Check failure rate alert
        long total = totalExecutions.get();
        if (total > 10) {  // Only alert if we have enough data
            double failureRate = (double) failedExecutions.get() / total * 100.0;
            if (failureRate > 20) {
                addAlert("FAILURE_RATE_HIGH", "Failure rate is high (" +
                        Math.round(failureRate) + "%). Check for script errors.", "WARNING");
            }
        }
    }

    /**
     * Add a health alert (avoids duplicates within 1 minute)
     */
    private void addAlert(String alertId, String message, String severity) {
        long now = System.currentTimeMillis();

        synchronized (activeAlerts) {
            // Check if this alert was recently added (within 1 minute)
            boolean alreadyActive = activeAlerts.stream()
                    .anyMatch(alert -> alert.alertId.equals(alertId) && now - alert.timestamp < 60000);

            if (!alreadyActive) {
                activeAlerts.add(new HealthAlert(now, alertId, message, severity));
                LOGGER.warn("Health Alert [{}]: {}", severity, message);
            }
        }
    }

    // Inner classes for new features (v1.16.0)

    /**
     * Tracks metrics for a specific script
     */
    private static class ScriptMetrics {
        private final String scriptIdentifier;
        private final AtomicLong totalExecutions = new AtomicLong(0);
        private final AtomicLong successfulExecutions = new AtomicLong(0);
        private final AtomicLong failedExecutions = new AtomicLong(0);
        private final AtomicLong totalExecutionTime = new AtomicLong(0);
        private final AtomicLong minExecutionTime = new AtomicLong(Long.MAX_VALUE);
        private final AtomicLong maxExecutionTime = new AtomicLong(0);
        private final Map<String, AtomicInteger> errorCounts = new ConcurrentHashMap<>();

        ScriptMetrics(String scriptIdentifier) {
            this.scriptIdentifier = scriptIdentifier;
        }

        void recordExecution(long executionTimeMs) {
            totalExecutions.incrementAndGet();
            successfulExecutions.incrementAndGet();
            totalExecutionTime.addAndGet(executionTimeMs);
            updateMinMax(executionTimeMs);
        }

        void recordFailure(String errorType, long executionTimeMs) {
            totalExecutions.incrementAndGet();
            failedExecutions.incrementAndGet();
            totalExecutionTime.addAndGet(executionTimeMs);
            errorCounts.computeIfAbsent(errorType, k -> new AtomicInteger(0)).incrementAndGet();
            updateMinMax(executionTimeMs);
        }

        private void updateMinMax(long value) {
            // Update min
            long currentMin;
            do {
                currentMin = minExecutionTime.get();
                if (value >= currentMin) break;
            } while (!minExecutionTime.compareAndSet(currentMin, value));

            // Update max
            long currentMax;
            do {
                currentMax = maxExecutionTime.get();
                if (value <= currentMax) break;
            } while (!maxExecutionTime.compareAndSet(currentMax, value));
        }

        long getTotalExecutions() {
            return totalExecutions.get();
        }

        Map<String, Object> toMap() {
            Map<String, Object> map = new HashMap<>();
            map.put("script_identifier", scriptIdentifier);
            map.put("total_executions", totalExecutions.get());
            map.put("successful_executions", successfulExecutions.get());
            map.put("failed_executions", failedExecutions.get());
            map.put("success_rate", totalExecutions.get() > 0 ?
                    (double) successfulExecutions.get() / totalExecutions.get() * 100.0 : 100.0);
            map.put("average_execution_time_ms", totalExecutions.get() > 0 ?
                    totalExecutionTime.get() / totalExecutions.get() : 0);
            map.put("min_execution_time_ms", minExecutionTime.get() == Long.MAX_VALUE ? 0 : minExecutionTime.get());
            map.put("max_execution_time_ms", maxExecutionTime.get());

            Map<String, Integer> errors = new HashMap<>();
            errorCounts.forEach((key, value) -> errors.put(key, value.get()));
            map.put("error_counts", errors);

            return map;
        }
    }

    /**
     * Snapshot of metrics at a specific point in time
     */
    private static class MetricSnapshot {
        final long timestamp;
        final long totalExecutions;
        final long successfulExecutions;
        final long failedExecutions;
        final long averageExecutionTimeMs;
        final int activeExecutions;
        final int poolSize;
        final double poolUtilization;

        MetricSnapshot(long timestamp, long totalExecutions, long successfulExecutions,
                      long failedExecutions, long averageExecutionTimeMs,
                      int activeExecutions, int poolSize) {
            this.timestamp = timestamp;
            this.totalExecutions = totalExecutions;
            this.successfulExecutions = successfulExecutions;
            this.failedExecutions = failedExecutions;
            this.averageExecutionTimeMs = averageExecutionTimeMs;
            this.activeExecutions = activeExecutions;
            this.poolSize = poolSize;
            this.poolUtilization = poolSize > 0 ? (double) activeExecutions / poolSize * 100.0 : 0.0;
        }

        Map<String, Object> toMap() {
            Map<String, Object> map = new HashMap<>();
            map.put("timestamp", timestamp);
            map.put("total_executions", totalExecutions);
            map.put("successful_executions", successfulExecutions);
            map.put("failed_executions", failedExecutions);
            map.put("average_execution_time_ms", averageExecutionTimeMs);
            map.put("active_executions", activeExecutions);
            map.put("pool_size", poolSize);
            map.put("pool_utilization", Math.round(poolUtilization * 100.0) / 100.0);
            return map;
        }
    }

    /**
     * Health alert when thresholds are exceeded
     */
    private static class HealthAlert {
        final long timestamp;
        final String alertId;
        final String message;
        final String severity;  // "WARNING", "CRITICAL"

        HealthAlert(long timestamp, String alertId, String message, String severity) {
            this.timestamp = timestamp;
            this.alertId = alertId;
            this.message = message;
            this.severity = severity;
        }

        Map<String, Object> toMap() {
            Map<String, Object> map = new HashMap<>();
            map.put("timestamp", timestamp);
            map.put("alert_id", alertId);
            map.put("message", message);
            map.put("severity", severity);
            return map;
        }
    }
}
