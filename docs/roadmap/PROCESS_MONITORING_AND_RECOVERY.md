# Process Monitoring and Recovery Enhancement Guide
## Python 3 Integration Module for Ignition

### Overview
This document provides detailed specifications for implementing robust process monitoring, health checking, and automatic recovery mechanisms for the Python 3 process pool.

## 1. Enhanced Health Monitoring System

### A. Multi-Level Health Checks

#### Implementation: HealthCheckStrategy
**Location:** `gateway/src/main/java/.../gateway/health/HealthCheckStrategy.java`

```java
public interface HealthCheckStrategy {
    HealthStatus checkHealth(Python3Executor executor);
    boolean shouldReplace(HealthStatus status);
    int getPriority();
}

public class CompositeHealthChecker {
    private final List<HealthCheckStrategy> strategies;

    public HealthStatus performHealthCheck(Python3Executor executor) {
        // Run all health checks and aggregate results
        // Return worst status found
    }
}
```

#### Health Check Levels

##### Level 1: Basic Liveness Check
```java
public class LivenessCheck implements HealthCheckStrategy {
    @Override
    public HealthStatus checkHealth(Python3Executor executor) {
        // Check if process is still running
        if (!executor.isProcessAlive()) {
            return HealthStatus.DEAD;
        }

        // Send ping command
        try {
            String result = executor.execute("print('ping')", 1000);
            if ("ping\n".equals(result.getOutput())) {
                return HealthStatus.HEALTHY;
            }
        } catch (TimeoutException e) {
            return HealthStatus.UNRESPONSIVE;
        }

        return HealthStatus.DEGRADED;
    }
}
```

##### Level 2: Performance Check
```java
public class PerformanceCheck implements HealthCheckStrategy {
    private static final long BASELINE_EXECUTION_TIME = 100; // ms

    @Override
    public HealthStatus checkHealth(Python3Executor executor) {
        long startTime = System.currentTimeMillis();

        // Execute benchmark script
        String benchmarkCode = """
            import time
            start = time.time()
            sum([i**2 for i in range(1000)])
            print(f"completed:{time.time()-start:.4f}")
            """;

        try {
            executor.execute(benchmarkCode, 5000);
            long executionTime = System.currentTimeMillis() - startTime;

            if (executionTime > BASELINE_EXECUTION_TIME * 5) {
                return HealthStatus.DEGRADED;
            }

            return HealthStatus.HEALTHY;
        } catch (Exception e) {
            return HealthStatus.UNHEALTHY;
        }
    }
}
```

##### Level 3: Memory Check
```java
public class MemoryCheck implements HealthCheckStrategy {
    @Override
    public HealthStatus checkHealth(Python3Executor executor) {
        String memoryCheckCode = """
            import psutil
            import os
            process = psutil.Process(os.getpid())
            memory_mb = process.memory_info().rss / 1024 / 1024
            print(f"memory:{memory_mb:.2f}")
            """;

        try {
            String output = executor.execute(memoryCheckCode, 2000).getOutput();
            double memoryMB = parseMemory(output);

            if (memoryMB > 500) {  // Configurable threshold
                return HealthStatus.MEMORY_HIGH;
            }

            return HealthStatus.HEALTHY;
        } catch (Exception e) {
            // psutil might not be installed
            return HealthStatus.UNKNOWN;
        }
    }
}
```

### B. Health Score System

#### Implementation: ExecutorHealthScore
```java
public class ExecutorHealthScore {
    private final Python3Executor executor;
    private final AtomicInteger score = new AtomicInteger(100);
    private final CircularFifoQueue<HealthEvent> history = new CircularFifoQueue<>(100);

    public void recordSuccess(long executionTime) {
        score.updateAndGet(s -> Math.min(100, s + 1));
        history.add(new HealthEvent(EventType.SUCCESS, executionTime));
    }

    public void recordFailure(Exception error) {
        int penalty = calculatePenalty(error);
        score.updateAndGet(s -> Math.max(0, s - penalty));
        history.add(new HealthEvent(EventType.FAILURE, error));
    }

    private int calculatePenalty(Exception error) {
        if (error instanceof TimeoutException) return 20;
        if (error instanceof OutOfMemoryError) return 50;
        if (error instanceof IOException) return 30;
        return 10;
    }

    public boolean shouldReplace() {
        return score.get() < 30;  // Configurable threshold
    }

    public HealthMetrics getMetrics() {
        return new HealthMetrics(
            score.get(),
            getSuccessRate(),
            getAverageExecutionTime(),
            getLastFailureTime()
        );
    }
}
```

## 2. Process Lifecycle Management

### A. Process Watchdog

#### Implementation: ProcessWatchdog
```java
public class ProcessWatchdog {
    private final ScheduledExecutorService scheduler;
    private final ProcessRegistry registry;
    private final AlertManager alertManager;

    public void startWatching(Python3Executor executor) {
        WatchedProcess process = new WatchedProcess(executor);
        registry.register(process);

        // Monitor at different intervals
        scheduler.scheduleAtFixedRate(
            () -> performQuickCheck(process),
            5, 5, TimeUnit.SECONDS
        );

        scheduler.scheduleAtFixedRate(
            () -> performDeepCheck(process),
            30, 30, TimeUnit.SECONDS
        );

        scheduler.scheduleAtFixedRate(
            () -> performResourceCheck(process),
            60, 60, TimeUnit.SECONDS
        );
    }

    private void performQuickCheck(WatchedProcess process) {
        if (!process.isAlive()) {
            handleProcessDeath(process);
        }
    }

    private void performDeepCheck(WatchedProcess process) {
        HealthStatus status = process.performHealthCheck();
        if (status.requiresAction()) {
            handleHealthIssue(process, status);
        }
    }

    private void handleProcessDeath(WatchedProcess process) {
        logger.error("Process {} died unexpectedly", process.getId());
        alertManager.sendAlert(AlertLevel.CRITICAL,
            "Python process died", process.getDetails());

        // Immediate replacement
        replaceProcess(process);
    }
}
```

### B. Graceful Process Replacement

#### Implementation: GracefulProcessReplacer
```java
public class GracefulProcessReplacer {
    private final Python3ProcessPool pool;

    public CompletableFuture<Python3Executor> replaceProcess(
            Python3Executor oldExecutor) {

        return CompletableFuture.supplyAsync(() -> {
            // Step 1: Create new executor
            Python3Executor newExecutor = createNewExecutor();

            // Step 2: Warm up the new executor
            warmUpExecutor(newExecutor);

            // Step 3: Mark old executor for replacement
            oldExecutor.markForReplacement();

            // Step 4: Wait for current execution to complete
            waitForCurrentExecution(oldExecutor);

            // Step 5: Swap in pool
            pool.replaceExecutor(oldExecutor, newExecutor);

            // Step 6: Cleanup old executor
            cleanupExecutor(oldExecutor);

            return newExecutor;
        });
    }

    private void warmUpExecutor(Python3Executor executor) {
        // Pre-load common libraries
        String warmupCode = """
            import sys
            import json
            import datetime
            import math
            # Pre-compile common operations
            compile("x = 1 + 1", "<string>", "exec")
            print("ready")
            """;

        executor.execute(warmupCode, 5000);
    }
}
```

## 3. Circuit Breaker Pattern

### Implementation: ProcessPoolCircuitBreaker
```java
public class ProcessPoolCircuitBreaker {
    private enum State {
        CLOSED,      // Normal operation
        OPEN,        // Failing, reject requests
        HALF_OPEN    // Testing recovery
    }

    private State state = State.CLOSED;
    private final AtomicInteger failureCount = new AtomicInteger(0);
    private final AtomicLong lastFailureTime = new AtomicLong(0);

    // Configuration
    private final int failureThreshold = 5;
    private final long timeout = 60000; // 60 seconds
    private final int successThreshold = 3;

    public ExecutionResult executeWithCircuitBreaker(
            Supplier<ExecutionResult> execution) {

        if (state == State.OPEN) {
            if (shouldAttemptReset()) {
                state = State.HALF_OPEN;
            } else {
                throw new CircuitOpenException("Circuit breaker is open");
            }
        }

        try {
            ExecutionResult result = execution.get();
            onSuccess();
            return result;
        } catch (Exception e) {
            onFailure();
            throw e;
        }
    }

    private void onSuccess() {
        failureCount.set(0);
        if (state == State.HALF_OPEN) {
            state = State.CLOSED;
            logger.info("Circuit breaker closed - system recovered");
        }
    }

    private void onFailure() {
        lastFailureTime.set(System.currentTimeMillis());
        int failures = failureCount.incrementAndGet();

        if (failures >= failureThreshold) {
            state = State.OPEN;
            logger.warn("Circuit breaker opened after {} failures", failures);
            scheduleReset();
        }
    }
}
```

## 4. Resource Management

### A. Process Resource Monitor

#### Implementation: ResourceMonitor
```java
public class ResourceMonitor {
    private final Map<Python3Executor, ResourceMetrics> metrics =
        new ConcurrentHashMap<>();

    public class ResourceMetrics {
        private final RollingWindow cpuUsage = new RollingWindow(60);
        private final RollingWindow memoryUsage = new RollingWindow(60);
        private final AtomicLong totalExecutions = new AtomicLong(0);
        private final AtomicLong totalCpuTime = new AtomicLong(0);

        public void recordExecution(long cpuTime, long memoryUsed) {
            cpuUsage.add(cpuTime);
            memoryUsage.add(memoryUsed);
            totalExecutions.incrementAndGet();
            totalCpuTime.addAndGet(cpuTime);
        }

        public boolean isOverloaded() {
            return cpuUsage.getAverage() > 1000 ||  // 1 second average
                   memoryUsage.getAverage() > 400_000_000;  // 400MB
        }
    }

    public void monitorExecution(Python3Executor executor,
                                  Runnable execution) {
        long startCpu = getProcessCpuTime(executor);
        long startMem = getProcessMemory(executor);

        execution.run();

        long cpuUsed = getProcessCpuTime(executor) - startCpu;
        long memUsed = getProcessMemory(executor) - startMem;

        metrics.get(executor).recordExecution(cpuUsed, memUsed);
    }
}
```

### B. Adaptive Pool Sizing

#### Implementation: AdaptivePoolManager
```java
public class AdaptivePoolManager {
    private final Python3ProcessPool pool;
    private final MetricsCollector metrics;

    // Configuration
    private final int minPoolSize = 2;
    private final int maxPoolSize = 10;
    private final double targetUtilization = 0.7;

    public void adjustPoolSize() {
        PoolStatistics stats = metrics.getPoolStats();
        double utilization = stats.getUtilization();
        double avgWaitTime = stats.getAverageWaitTime();

        if (utilization > 0.9 && avgWaitTime > 1000) {
            // High load - increase pool
            scaleUp();
        } else if (utilization < 0.3 && pool.size() > minPoolSize) {
            // Low load - decrease pool
            scaleDown();
        }
    }

    private void scaleUp() {
        int currentSize = pool.size();
        int newSize = Math.min(currentSize + 2, maxPoolSize);

        logger.info("Scaling up pool from {} to {}", currentSize, newSize);

        for (int i = currentSize; i < newSize; i++) {
            pool.addExecutor(createNewExecutor());
        }
    }

    private void scaleDown() {
        int currentSize = pool.size();
        int newSize = Math.max(currentSize - 1, minPoolSize);

        logger.info("Scaling down pool from {} to {}", currentSize, newSize);

        // Remove least healthy executors
        pool.removeExecutors(currentSize - newSize,
            Comparator.comparing(e -> e.getHealthScore()));
    }
}
```

## 5. Monitoring and Alerting

### A. Metrics Collection

#### Implementation: MetricsCollector
```java
public class MetricsCollector {
    private final MeterRegistry meterRegistry;

    // Counters
    private final Counter executionCounter;
    private final Counter errorCounter;
    private final Counter timeoutCounter;

    // Gauges
    private final AtomicInteger poolSize;
    private final AtomicInteger availableExecutors;
    private final AtomicInteger unhealthyExecutors;

    // Timers
    private final Timer executionTimer;
    private final Timer queueWaitTimer;

    // Histograms
    private final DistributionSummary outputSizeDistribution;

    public void recordExecution(ExecutionContext context) {
        executionCounter.increment();

        executionTimer.record(context.getExecutionTime(),
            TimeUnit.MILLISECONDS);

        if (context.hasError()) {
            errorCounter.increment();
            if (context.isTimeout()) {
                timeoutCounter.increment();
            }
        }

        outputSizeDistribution.record(context.getOutputSize());
    }

    public void exportMetrics() {
        // Export to Prometheus
        PrometheusExporter.export(meterRegistry);

        // Export to CloudWatch
        CloudWatchExporter.export(meterRegistry);

        // Export to internal monitoring
        InternalMonitor.record(getSnapshot());
    }
}
```

### B. Alert Manager

#### Implementation: AlertManager
```java
public class AlertManager {
    private final List<AlertChannel> channels;
    private final AlertThrottler throttler;

    public void sendAlert(AlertLevel level, String title,
                          Map<String, Object> context) {

        Alert alert = new Alert(level, title, context);

        // Throttle to prevent alert storms
        if (throttler.shouldSuppress(alert)) {
            return;
        }

        // Route to appropriate channels based on severity
        for (AlertChannel channel : channels) {
            if (channel.handles(level)) {
                channel.send(alert);
            }
        }
    }
}

public class EmailAlertChannel implements AlertChannel {
    @Override
    public void send(Alert alert) {
        String subject = String.format("[%s] Python3 Module: %s",
            alert.getLevel(), alert.getTitle());

        String body = formatAlertBody(alert);

        emailService.send(getRecipients(alert.getLevel()),
            subject, body);
    }
}

public class SlackAlertChannel implements AlertChannel {
    @Override
    public void send(Alert alert) {
        SlackMessage message = new SlackMessage()
            .withColor(getColor(alert.getLevel()))
            .withTitle(alert.getTitle())
            .withFields(alert.getContext());

        slackClient.postMessage(getChannel(alert.getLevel()), message);
    }
}
```

## 6. Recovery Strategies

### A. Automatic Recovery Actions

#### Implementation: RecoveryOrchestrator
```java
public class RecoveryOrchestrator {
    private final Map<FailureType, RecoveryStrategy> strategies;

    public RecoveryOrchestrator() {
        strategies.put(FailureType.PROCESS_CRASH,
            new ProcessRestartStrategy());
        strategies.put(FailureType.MEMORY_LEAK,
            new MemoryRecoveryStrategy());
        strategies.put(FailureType.DEADLOCK,
            new DeadlockRecoveryStrategy());
        strategies.put(FailureType.RESOURCE_EXHAUSTION,
            new ResourceRecoveryStrategy());
    }

    public void handleFailure(FailureContext context) {
        FailureType type = detectFailureType(context);
        RecoveryStrategy strategy = strategies.get(type);

        if (strategy != null) {
            RecoveryResult result = strategy.recover(context);

            if (result.isSuccessful()) {
                logger.info("Recovery successful: {}", result);
            } else {
                escalateFailure(context, result);
            }
        }
    }
}

public class ProcessRestartStrategy implements RecoveryStrategy {
    @Override
    public RecoveryResult recover(FailureContext context) {
        Python3Executor failed = context.getFailedExecutor();

        // Step 1: Kill the process if still running
        failed.killProcess();

        // Step 2: Clear any resources
        failed.cleanup();

        // Step 3: Start new process
        try {
            failed.startProcess();

            // Step 4: Validate new process
            if (failed.isHealthy()) {
                return RecoveryResult.success("Process restarted");
            }
        } catch (Exception e) {
            return RecoveryResult.failure("Restart failed", e);
        }

        return RecoveryResult.failure("Process unhealthy after restart");
    }
}
```

### B. Fallback Mechanisms

#### Implementation: FallbackExecutor
```java
public class FallbackExecutor {
    private final List<ExecutionStrategy> strategies;

    public ExecutionResult executeWithFallback(String code,
                                                Map<String, Object> variables) {

        for (ExecutionStrategy strategy : strategies) {
            try {
                return strategy.execute(code, variables);
            } catch (Exception e) {
                logger.warn("Strategy {} failed: {}",
                    strategy.getName(), e.getMessage());
            }
        }

        // All strategies failed
        return ExecutionResult.failure("All execution strategies failed");
    }
}

public class PrimaryPoolStrategy implements ExecutionStrategy {
    @Override
    public ExecutionResult execute(String code, Map<String, Object> vars) {
        return pool.execute(code, vars);
    }
}

public class SecondaryPoolStrategy implements ExecutionStrategy {
    // Use a backup pool with different configuration
}

public class DirectExecutionStrategy implements ExecutionStrategy {
    // Execute directly without pool (emergency fallback)
}
```

## 7. Diagnostic Tools

### A. Health Dashboard

#### Implementation: HealthDashboard
```java
public class HealthDashboard {

    public DashboardData generateDashboard() {
        return new DashboardData()
            .withPoolHealth(getPoolHealth())
            .withExecutorStatuses(getExecutorStatuses())
            .withPerformanceMetrics(getPerformanceMetrics())
            .withRecentFailures(getRecentFailures())
            .withResourceUsage(getResourceUsage());
    }

    private PoolHealth getPoolHealth() {
        return new PoolHealth()
            .withTotalExecutors(pool.size())
            .withHealthyExecutors(pool.getHealthyCount())
            .withAvailableExecutors(pool.getAvailableCount())
            .withQueuedRequests(pool.getQueueSize())
            .withOverallScore(calculateOverallScore());
    }
}
```

### B. Diagnostic Commands

#### Implementation: DiagnosticCommands
```java
public class DiagnosticCommands {

    public String runDiagnostic(String command) {
        switch (command) {
            case "health":
                return generateHealthReport();
            case "performance":
                return runPerformanceTest();
            case "memory":
                return analyzeMemoryUsage();
            case "threads":
                return dumpThreads();
            case "history":
                return getExecutionHistory();
            default:
                return "Unknown diagnostic command";
        }
    }

    private String generateHealthReport() {
        StringBuilder report = new StringBuilder();
        report.append("=== Python3 Process Pool Health Report ===\n");
        report.append(String.format("Time: %s\n", LocalDateTime.now()));
        report.append(String.format("Pool Size: %d\n", pool.size()));
        report.append(String.format("Healthy: %d\n", pool.getHealthyCount()));

        for (Python3Executor executor : pool.getAllExecutors()) {
            report.append(String.format("\nExecutor %s:\n", executor.getId()));
            report.append(String.format("  Status: %s\n", executor.getStatus()));
            report.append(String.format("  Health Score: %d\n",
                executor.getHealthScore()));
            report.append(String.format("  Executions: %d\n",
                executor.getExecutionCount()));
            report.append(String.format("  Last Used: %s\n",
                executor.getLastUsed()));
        }

        return report.toString();
    }
}
```

## 8. Configuration

### application.yml
```yaml
python3:
  pool:
    min-size: 2
    max-size: 10
    initial-size: 3

  health:
    check-interval: 30s
    quick-check-interval: 5s
    failure-threshold: 5
    recovery-threshold: 3

  monitoring:
    enabled: true
    export-interval: 60s
    metrics-retention: 7d

  alerts:
    enabled: true
    channels:
      - email
      - slack
    throttle:
      max-per-hour: 10

  circuit-breaker:
    enabled: true
    failure-threshold: 5
    timeout: 60s
    success-threshold: 3

  recovery:
    max-restart-attempts: 3
    restart-delay: 5s
    escalation-timeout: 5m
```

## 9. Implementation Checklist

### Phase 1: Core Monitoring (Days 1-2)
- [ ] Implement HealthCheckStrategy interface
- [ ] Create LivenessCheck implementation
- [ ] Create PerformanceCheck implementation
- [ ] Implement ExecutorHealthScore
- [ ] Add health check scheduling to pool

### Phase 2: Recovery Mechanisms (Days 3-4)
- [ ] Implement ProcessWatchdog
- [ ] Create GracefulProcessReplacer
- [ ] Implement RecoveryOrchestrator
- [ ] Add recovery strategies
- [ ] Test automatic recovery

### Phase 3: Circuit Breaker (Day 5)
- [ ] Implement CircuitBreaker pattern
- [ ] Add circuit breaker to pool
- [ ] Configure thresholds
- [ ] Test circuit breaker behavior

### Phase 4: Metrics and Alerting (Days 6-7)
- [ ] Set up MetricsCollector
- [ ] Implement AlertManager
- [ ] Create alert channels
- [ ] Add metric exporters
- [ ] Create health dashboard

### Phase 5: Advanced Features (Days 8-10)
- [ ] Implement AdaptivePoolManager
- [ ] Add ResourceMonitor
- [ ] Create diagnostic commands
- [ ] Implement fallback strategies
- [ ] Complete integration testing

## 10. Testing Strategy

### Unit Tests Required
- Health check strategies
- Circuit breaker state transitions
- Recovery orchestration
- Alert throttling
- Metric collection

### Integration Tests Required
- End-to-end process failure and recovery
- Circuit breaker with real pool
- Alert delivery
- Metric export
- Adaptive scaling

### Chaos Engineering Tests
- Random process kills
- Memory pressure
- CPU starvation
- Network issues
- Cascading failures

## Success Metrics

- **MTTR (Mean Time To Recovery):** < 30 seconds
- **False Positive Rate:** < 5% for health checks
- **Alert Accuracy:** > 95%
- **Recovery Success Rate:** > 99%
- **Performance Impact:** < 5% overhead
- **Pool Availability:** > 99.9%
