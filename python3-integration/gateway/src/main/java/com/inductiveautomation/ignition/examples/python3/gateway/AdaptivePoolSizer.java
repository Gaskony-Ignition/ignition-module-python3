package com.inductiveautomation.ignition.examples.python3.gateway;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Adaptive Pool Sizer - Automatically adjusts process pool size based on load.
 *
 * <p>Monitors pool utilization and queue depth to determine when to scale up or down.
 * Prevents rapid scaling oscillations using cooldown periods and hysteresis.
 *
 * <h2>Scaling Logic:</h2>
 * <ul>
 *   <li><b>Scale Up:</b> When utilization > scaleUpThreshold for scaleUpDelaySec seconds</li>
 *   <li><b>Scale Down:</b> When utilization < scaleDownThreshold for scaleDownDelaySec seconds</li>
 * </ul>
 *
 * <h2>Configuration:</h2>
 * <pre>{@code
 * AdaptivePoolSizer sizer = new AdaptivePoolSizer.Builder(pool)
 *     .minPoolSize(3)
 *     .maxPoolSize(15)
 *     .scaleUpThreshold(80)      // Scale up when > 80% utilized
 *     .scaleDownThreshold(30)    // Scale down when < 30% utilized
 *     .scaleUpDelay(30)          // Wait 30s before scaling up
 *     .scaleDownDelay(300)       // Wait 5min before scaling down
 *     .checkInterval(15)         // Check every 15 seconds
 *     .build();
 * sizer.start();
 * }</pre>
 *
 * @since v2.15.0 (Phase 3 Week 1-2: Advanced Monitoring)
 */
public class AdaptivePoolSizer {

    private static final Logger LOGGER = LoggerFactory.getLogger(AdaptivePoolSizer.class);

    // Configuration
    private final Python3ProcessPool pool;
    private final int minPoolSize;
    private final int maxPoolSize;
    private final double scaleUpThreshold;      // Utilization % to trigger scale up
    private final double scaleDownThreshold;    // Utilization % to trigger scale down
    private final int scaleUpDelaySec;          // Seconds to wait before scaling up
    private final int scaleDownDelaySec;        // Seconds to wait before scaling down
    private final int checkIntervalSec;         // Seconds between checks

    // State tracking
    private final ScheduledExecutorService scheduler;
    private volatile boolean isRunning = false;
    private final AtomicInteger consecutiveHighUtilization = new AtomicInteger(0);
    private final AtomicInteger consecutiveLowUtilization = new AtomicInteger(0);
    private volatile long lastScaleUpTime = 0;
    private volatile long lastScaleDownTime = 0;

    // Cooldown to prevent rapid scaling oscillations
    private final long scaleCooldownMs;  // Minimum time between any scaling operations (default: 60 seconds)

    private AdaptivePoolSizer(Builder builder) {
        this.pool = builder.pool;
        this.minPoolSize = builder.minPoolSize;
        this.maxPoolSize = builder.maxPoolSize;
        this.scaleUpThreshold = builder.scaleUpThreshold;
        this.scaleDownThreshold = builder.scaleDownThreshold;
        this.scaleUpDelaySec = builder.scaleUpDelaySec;
        this.scaleDownDelaySec = builder.scaleDownDelaySec;
        this.checkIntervalSec = builder.checkIntervalSec;
        this.scaleCooldownMs = builder.scaleCooldownMs;

        this.scheduler = new ScheduledThreadPoolExecutor(1, r -> {
            Thread t = new Thread(r, "AdaptivePoolSizer");
            t.setDaemon(true);
            return t;
        });

        LOGGER.info("AdaptivePoolSizer created: min={}, max={}, scaleUpThreshold={}%, scaleDownThreshold={}%",
                minPoolSize, maxPoolSize, scaleUpThreshold, scaleDownThreshold);
    }

    /**
     * Start adaptive pool sizing.
     */
    public void start() {
        if (isRunning) {
            LOGGER.warn("AdaptivePoolSizer already running");
            return;
        }

        LOGGER.info("Starting AdaptivePoolSizer (check interval: {}s)", checkIntervalSec);
        isRunning = true;

        scheduler.scheduleAtFixedRate(
                this::checkAndResize,
                checkIntervalSec,  // Initial delay
                checkIntervalSec,  // Period
                TimeUnit.SECONDS
        );
    }

    /**
     * Stop adaptive pool sizing.
     */
    public void stop() {
        if (!isRunning) {
            return;
        }

        LOGGER.info("Stopping AdaptivePoolSizer");
        isRunning = false;
        scheduler.shutdown();

        try {
            if (!scheduler.awaitTermination(5, TimeUnit.SECONDS)) {
                scheduler.shutdownNow();
            }
        } catch (InterruptedException e) {
            scheduler.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }

    /**
     * Check pool metrics and resize if needed.
     */
    private void checkAndResize() {
        try {
            Python3ProcessPool.PoolStats stats = pool.getStats();
            double utilization = calculateUtilization(stats);

            LOGGER.debug("Pool check: size={}, available={}, inUse={}, utilization={}%",
                    stats.totalSize, stats.available, stats.inUse, String.format("%.1f", utilization));

            // Check if we should scale up
            if (shouldScaleUp(stats, utilization)) {
                scaleUp(stats);
            }
            // Check if we should scale down
            else if (shouldScaleDown(stats, utilization)) {
                scaleDown(stats);
            }
            // Reset counters if utilization is in the middle range
            else {
                consecutiveHighUtilization.set(0);
                consecutiveLowUtilization.set(0);
            }

        } catch (Exception e) {
            LOGGER.error("Error during adaptive pool sizing check", e);
        }
    }

    /**
     * Calculate pool utilization percentage.
     */
    private double calculateUtilization(Python3ProcessPool.PoolStats stats) {
        if (stats.totalSize == 0) {
            return 0.0;
        }
        return (stats.inUse * 100.0) / stats.totalSize;
    }

    /**
     * Determine if pool should scale up.
     */
    private boolean shouldScaleUp(Python3ProcessPool.PoolStats stats, double utilization) {
        // Don't scale up if already at max size
        if (stats.totalSize >= maxPoolSize) {
            consecutiveHighUtilization.set(0);
            return false;
        }

        // Don't scale up if in cooldown period
        long timeSinceLastScale = System.currentTimeMillis() - Math.max(lastScaleUpTime, lastScaleDownTime);
        if (timeSinceLastScale < scaleCooldownMs) {
            return false;
        }

        // Check if utilization is above threshold
        if (utilization > scaleUpThreshold) {
            int consecutive = consecutiveHighUtilization.incrementAndGet();
            consecutiveLowUtilization.set(0);

            // Calculate how many checks needed based on delay
            int checksNeeded = scaleUpDelaySec / checkIntervalSec;
            if (checksNeeded < 1) {
                checksNeeded = 1;
            }

            if (consecutive >= checksNeeded) {
                LOGGER.info("Scale up triggered: utilization={}% for {} checks (threshold={}%)",
                        String.format("%.1f", utilization), consecutive, scaleUpThreshold);
                return true;
            } else {
                LOGGER.debug("High utilization detected ({}/{}): {}%",
                        consecutive, checksNeeded, String.format("%.1f", utilization));
            }
        } else {
            consecutiveHighUtilization.set(0);
        }

        return false;
    }

    /**
     * Determine if pool should scale down.
     */
    private boolean shouldScaleDown(Python3ProcessPool.PoolStats stats, double utilization) {
        // Don't scale down if already at min size
        if (stats.totalSize <= minPoolSize) {
            consecutiveLowUtilization.set(0);
            return false;
        }

        // Don't scale down if in cooldown period
        long timeSinceLastScale = System.currentTimeMillis() - Math.max(lastScaleUpTime, lastScaleDownTime);
        if (timeSinceLastScale < scaleCooldownMs) {
            return false;
        }

        // Check if utilization is below threshold
        if (utilization < scaleDownThreshold) {
            int consecutive = consecutiveLowUtilization.incrementAndGet();
            consecutiveHighUtilization.set(0);

            // Calculate how many checks needed based on delay
            int checksNeeded = scaleDownDelaySec / checkIntervalSec;
            if (checksNeeded < 1) {
                checksNeeded = 1;
            }

            if (consecutive >= checksNeeded) {
                LOGGER.info("Scale down triggered: utilization={}% for {} checks (threshold={}%)",
                        String.format("%.1f", utilization), consecutive, scaleDownThreshold);
                return true;
            } else {
                LOGGER.debug("Low utilization detected ({}/{}): {}%",
                        consecutive, checksNeeded, String.format("%.1f", utilization));
            }
        } else {
            consecutiveLowUtilization.set(0);
        }

        return false;
    }

    /**
     * Scale pool up by one executor.
     */
    private void scaleUp(Python3ProcessPool.PoolStats stats) {
        int newSize = Math.min(stats.totalSize + 1, maxPoolSize);
        LOGGER.info("Scaling up pool: {} → {} (max={})", stats.totalSize, newSize, maxPoolSize);

        try {
            pool.resizePool(newSize);
            lastScaleUpTime = System.currentTimeMillis();
            consecutiveHighUtilization.set(0);
            LOGGER.info("Pool scaled up successfully to {}", newSize);
        } catch (Exception e) {
            LOGGER.error("Failed to scale up pool to {}", newSize, e);
        }
    }

    /**
     * Scale pool down by one executor.
     */
    private void scaleDown(Python3ProcessPool.PoolStats stats) {
        int newSize = Math.max(stats.totalSize - 1, minPoolSize);
        LOGGER.info("Scaling down pool: {} → {} (min={})", stats.totalSize, newSize, minPoolSize);

        try {
            pool.resizePool(newSize);
            lastScaleDownTime = System.currentTimeMillis();
            consecutiveLowUtilization.set(0);
            LOGGER.info("Pool scaled down successfully to {}", newSize);
        } catch (Exception e) {
            LOGGER.error("Failed to scale down pool to {}", newSize, e);
        }
    }

    /**
     * Get current configuration as a string.
     */
    public String getConfiguration() {
        return String.format("AdaptivePoolSizer[min=%d, max=%d, scaleUp>%.0f%%, scaleDown<%.0f%%, " +
                        "scaleUpDelay=%ds, scaleDownDelay=%ds, checkInterval=%ds, running=%s]",
                minPoolSize, maxPoolSize, scaleUpThreshold, scaleDownThreshold,
                scaleUpDelaySec, scaleDownDelaySec, checkIntervalSec, isRunning);
    }

    /**
     * Check if adaptive sizing is running.
     */
    public boolean isRunning() {
        return isRunning;
    }

    /**
     * Builder for AdaptivePoolSizer.
     */
    public static class Builder {
        private final Python3ProcessPool pool;
        private int minPoolSize = 3;
        private int maxPoolSize = 15;
        private double scaleUpThreshold = 80.0;      // 80% utilization
        private double scaleDownThreshold = 30.0;    // 30% utilization
        private int scaleUpDelaySec = 30;            // 30 seconds
        private int scaleDownDelaySec = 300;         // 5 minutes
        private int checkIntervalSec = 15;           // 15 seconds
        private long scaleCooldownMs = 60000;        // 60 seconds

        public Builder(Python3ProcessPool pool) {
            if (pool == null) {
                throw new IllegalArgumentException("Pool cannot be null");
            }
            this.pool = pool;
        }

        public Builder minPoolSize(int minPoolSize) {
            if (minPoolSize < 1 || minPoolSize > 20) {
                throw new IllegalArgumentException("Min pool size must be between 1 and 20");
            }
            this.minPoolSize = minPoolSize;
            return this;
        }

        public Builder maxPoolSize(int maxPoolSize) {
            if (maxPoolSize < 1 || maxPoolSize > 20) {
                throw new IllegalArgumentException("Max pool size must be between 1 and 20");
            }
            this.maxPoolSize = maxPoolSize;
            return this;
        }

        public Builder scaleUpThreshold(double scaleUpThreshold) {
            if (scaleUpThreshold < 0 || scaleUpThreshold > 100) {
                throw new IllegalArgumentException("Scale up threshold must be between 0 and 100");
            }
            this.scaleUpThreshold = scaleUpThreshold;
            return this;
        }

        public Builder scaleDownThreshold(double scaleDownThreshold) {
            if (scaleDownThreshold < 0 || scaleDownThreshold > 100) {
                throw new IllegalArgumentException("Scale down threshold must be between 0 and 100");
            }
            this.scaleDownThreshold = scaleDownThreshold;
            return this;
        }

        public Builder scaleUpDelay(int scaleUpDelaySec) {
            if (scaleUpDelaySec < 1) {
                throw new IllegalArgumentException("Scale up delay must be at least 1 second");
            }
            this.scaleUpDelaySec = scaleUpDelaySec;
            return this;
        }

        public Builder scaleDownDelay(int scaleDownDelaySec) {
            if (scaleDownDelaySec < 1) {
                throw new IllegalArgumentException("Scale down delay must be at least 1 second");
            }
            this.scaleDownDelaySec = scaleDownDelaySec;
            return this;
        }

        public Builder checkInterval(int checkIntervalSec) {
            if (checkIntervalSec < 1) {
                throw new IllegalArgumentException("Check interval must be at least 1 second");
            }
            this.checkIntervalSec = checkIntervalSec;
            return this;
        }

        public Builder scaleCooldown(long scaleCooldownMs) {
            if (scaleCooldownMs < 0) {
                throw new IllegalArgumentException("Scale cooldown must be >= 0");
            }
            this.scaleCooldownMs = scaleCooldownMs;
            return this;
        }

        public AdaptivePoolSizer build() {
            // Validate min < max
            if (minPoolSize > maxPoolSize) {
                throw new IllegalArgumentException(
                        String.format("Min pool size (%d) must be <= max pool size (%d)",
                                minPoolSize, maxPoolSize));
            }

            // Validate scale down threshold < scale up threshold (hysteresis)
            if (scaleDownThreshold >= scaleUpThreshold) {
                throw new IllegalArgumentException(
                        String.format("Scale down threshold (%.0f%%) must be < scale up threshold (%.0f%%)",
                                scaleDownThreshold, scaleUpThreshold));
            }

            return new AdaptivePoolSizer(this);
        }
    }
}
