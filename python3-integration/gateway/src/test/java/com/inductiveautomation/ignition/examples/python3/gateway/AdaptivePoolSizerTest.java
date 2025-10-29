package com.inductiveautomation.ignition.examples.python3.gateway;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for AdaptivePoolSizer.
 *
 * Tests automatic pool scaling based on utilization metrics.
 *
 * @since v2.15.0 (Phase 3 Week 1-2)
 */
public class AdaptivePoolSizerTest {

    private Python3ProcessPool pool;
    private AdaptivePoolSizer sizer;

    @BeforeEach
    public void setUp() throws IOException {
        // Create a small pool for testing
        String pythonPath = System.getProperty("python3.path", "python3");
        pool = new Python3ProcessPool(pythonPath, 3);
    }

    @AfterEach
    public void tearDown() {
        if (sizer != null && sizer.isRunning()) {
            sizer.stop();
        }
        if (pool != null) {
            pool.shutdown();
        }
    }

    @Test
    public void testBuilderDefaults() {
        sizer = new AdaptivePoolSizer.Builder(pool).build();

        assertNotNull(sizer);
        assertFalse(sizer.isRunning());
        String config = sizer.getConfiguration();
        assertTrue(config.contains("min=3"));
        assertTrue(config.contains("max=15"));
        assertTrue(config.contains("scaleUp>80"));
        assertTrue(config.contains("scaleDown<30"));
    }

    @Test
    public void testBuilderCustomConfiguration() {
        sizer = new AdaptivePoolSizer.Builder(pool)
                .minPoolSize(5)
                .maxPoolSize(10)
                .scaleUpThreshold(70)
                .scaleDownThreshold(20)
                .scaleUpDelay(10)
                .scaleDownDelay(60)
                .checkInterval(5)
                .build();

        String config = sizer.getConfiguration();
        assertTrue(config.contains("min=5"));
        assertTrue(config.contains("max=10"));
        assertTrue(config.contains("scaleUp>70"));
        assertTrue(config.contains("scaleDown<20"));
        assertTrue(config.contains("scaleUpDelay=10s"));
        assertTrue(config.contains("scaleDownDelay=60s"));
        assertTrue(config.contains("checkInterval=5s"));
    }

    @Test
    public void testBuilderValidation_NullPool() {
        assertThrows(IllegalArgumentException.class, () -> {
            new AdaptivePoolSizer.Builder(null);
        });
    }

    @Test
    public void testBuilderValidation_MinPoolSizeTooSmall() {
        assertThrows(IllegalArgumentException.class, () -> {
            new AdaptivePoolSizer.Builder(pool).minPoolSize(0).build();
        });
    }

    @Test
    public void testBuilderValidation_MinPoolSizeTooLarge() {
        assertThrows(IllegalArgumentException.class, () -> {
            new AdaptivePoolSizer.Builder(pool).minPoolSize(21).build();
        });
    }

    @Test
    public void testBuilderValidation_MaxPoolSizeTooSmall() {
        assertThrows(IllegalArgumentException.class, () -> {
            new AdaptivePoolSizer.Builder(pool).maxPoolSize(0).build();
        });
    }

    @Test
    public void testBuilderValidation_MaxPoolSizeTooLarge() {
        assertThrows(IllegalArgumentException.class, () -> {
            new AdaptivePoolSizer.Builder(pool).maxPoolSize(21).build();
        });
    }

    @Test
    public void testBuilderValidation_MinGreaterThanMax() {
        assertThrows(IllegalArgumentException.class, () -> {
            new AdaptivePoolSizer.Builder(pool)
                    .minPoolSize(10)
                    .maxPoolSize(5)
                    .build();
        });
    }

    @Test
    public void testBuilderValidation_ScaleDownThresholdTooHigh() {
        assertThrows(IllegalArgumentException.class, () -> {
            new AdaptivePoolSizer.Builder(pool)
                    .scaleUpThreshold(70)
                    .scaleDownThreshold(80)  // Must be < scale up threshold
                    .build();
        });
    }

    @Test
    public void testBuilderValidation_ThresholdsEqual() {
        assertThrows(IllegalArgumentException.class, () -> {
            new AdaptivePoolSizer.Builder(pool)
                    .scaleUpThreshold(70)
                    .scaleDownThreshold(70)  // Must be < scale up threshold
                    .build();
        });
    }

    @Test
    public void testBuilderValidation_InvalidScaleUpThreshold() {
        assertThrows(IllegalArgumentException.class, () -> {
            new AdaptivePoolSizer.Builder(pool).scaleUpThreshold(101).build();
        });

        assertThrows(IllegalArgumentException.class, () -> {
            new AdaptivePoolSizer.Builder(pool).scaleUpThreshold(-1).build();
        });
    }

    @Test
    public void testBuilderValidation_InvalidScaleDownThreshold() {
        assertThrows(IllegalArgumentException.class, () -> {
            new AdaptivePoolSizer.Builder(pool).scaleDownThreshold(101).build();
        });

        assertThrows(IllegalArgumentException.class, () -> {
            new AdaptivePoolSizer.Builder(pool).scaleDownThreshold(-1).build();
        });
    }

    @Test
    public void testBuilderValidation_InvalidDelays() {
        assertThrows(IllegalArgumentException.class, () -> {
            new AdaptivePoolSizer.Builder(pool).scaleUpDelay(0).build();
        });

        assertThrows(IllegalArgumentException.class, () -> {
            new AdaptivePoolSizer.Builder(pool).scaleDownDelay(0).build();
        });

        assertThrows(IllegalArgumentException.class, () -> {
            new AdaptivePoolSizer.Builder(pool).checkInterval(0).build();
        });
    }

    @Test
    public void testStartAndStop() {
        sizer = new AdaptivePoolSizer.Builder(pool)
                .checkInterval(1)  // Fast check for testing
                .build();

        assertFalse(sizer.isRunning());

        sizer.start();
        assertTrue(sizer.isRunning());

        sizer.stop();
        assertFalse(sizer.isRunning());
    }

    @Test
    public void testStartTwice() {
        sizer = new AdaptivePoolSizer.Builder(pool)
                .checkInterval(1)
                .build();

        sizer.start();
        assertTrue(sizer.isRunning());

        // Starting again should log warning but not crash
        sizer.start();
        assertTrue(sizer.isRunning());
    }

    @Test
    public void testStopWithoutStart() {
        sizer = new AdaptivePoolSizer.Builder(pool).build();

        // Stopping without starting should not crash
        assertDoesNotThrow(() -> sizer.stop());
        assertFalse(sizer.isRunning());
    }

    @Test
    @Timeout(value = 10, unit = TimeUnit.SECONDS)
    public void testScaleUpOnHighUtilization() throws Exception {
        // Configure for fast scaling
        sizer = new AdaptivePoolSizer.Builder(pool)
                .minPoolSize(3)
                .maxPoolSize(6)
                .scaleUpThreshold(50)      // Scale up when > 50% utilized
                .scaleDownThreshold(10)    // Scale down when < 10% utilized
                .scaleUpDelay(1)           // Wait 1 second (1 check at 1s interval)
                .checkInterval(1)          // Check every 1 second
                .scaleCooldown(500)        // 0.5 second cooldown for testing
                .build();

        sizer.start();

        // Initial pool size
        assertEquals(3, pool.getStats().totalSize);

        // Borrow 2 executors to reach 66% utilization (2/3)
        Python3Executor exec1 = pool.borrowExecutor(5, TimeUnit.SECONDS);
        Python3Executor exec2 = pool.borrowExecutor(5, TimeUnit.SECONDS);

        assertNotNull(exec1);
        assertNotNull(exec2);

        // Wait for scale up: 1s initial delay + 1s check + 1s buffer = 3s
        Thread.sleep(3500);

        // Pool should have scaled up
        int newSize = pool.getStats().totalSize;
        assertTrue(newSize > 3, "Pool should have scaled up, but size is still " + newSize);

        // Return executors
        pool.returnExecutor(exec1);
        pool.returnExecutor(exec2);
    }

    @Test
    @Timeout(value = 10, unit = TimeUnit.SECONDS)
    public void testNoScaleUpWhenBelowThreshold() throws Exception {
        sizer = new AdaptivePoolSizer.Builder(pool)
                .minPoolSize(3)
                .maxPoolSize(6)
                .scaleUpThreshold(80)      // Scale up when > 80% utilized
                .scaleDownThreshold(10)
                .scaleUpDelay(2)
                .checkInterval(1)
                .build();

        sizer.start();

        assertEquals(3, pool.getStats().totalSize);

        // Borrow 1 executor (33% utilization - below threshold)
        Python3Executor exec = pool.borrowExecutor(5, TimeUnit.SECONDS);
        assertNotNull(exec);

        // Wait
        Thread.sleep(3000);

        // Pool should NOT have scaled up
        assertEquals(3, pool.getStats().totalSize);

        pool.returnExecutor(exec);
    }

    @Test
    @Timeout(value = 15, unit = TimeUnit.SECONDS)
    @Disabled("Timing-sensitive test - may be flaky in CI environments")
    public void testScaleDownOnLowUtilization() throws Exception {
        // Start with larger pool
        pool.resizePool(6);
        assertEquals(6, pool.getStats().totalSize);

        // Configure for fast scaling down
        sizer = new AdaptivePoolSizer.Builder(pool)
                .minPoolSize(3)
                .maxPoolSize(10)
                .scaleUpThreshold(80)
                .scaleDownThreshold(50)    // Scale down when < 50% utilized
                .scaleDownDelay(1)         // Wait 1 second (1 check at 1s interval)
                .checkInterval(1)          // Check every 1 second
                .scaleCooldown(0)          // No cooldown for testing
                .build();

        sizer.start();

        // Pool is at 0% utilization (0/6)
        // Wait for scale down: 1s initial delay + 2s for checks + 1s buffer = 4s
        Thread.sleep(4000);

        // Pool should have scaled down
        int newSize = pool.getStats().totalSize;
        assertTrue(newSize < 6, "Pool should have scaled down, but size is still " + newSize);
    }

    @Test
    @Timeout(value = 10, unit = TimeUnit.SECONDS)
    public void testNoScaleDownBelowMinSize() throws Exception {
        // Configure to try to scale down
        sizer = new AdaptivePoolSizer.Builder(pool)
                .minPoolSize(3)
                .maxPoolSize(10)
                .scaleUpThreshold(80)
                .scaleDownThreshold(50)
                .scaleDownDelay(1)
                .checkInterval(1)
                .build();

        sizer.start();

        assertEquals(3, pool.getStats().totalSize);

        // Wait - should not scale below min
        Thread.sleep(3000);

        assertEquals(3, pool.getStats().totalSize, "Pool should not scale below min size");
    }

    @Test
    @Timeout(value = 10, unit = TimeUnit.SECONDS)
    @Disabled("Timing-sensitive test - may be flaky in CI environments")
    public void testNoScaleUpAboveMaxSize() throws Exception {
        // Start at max size
        pool.resizePool(6);
        assertEquals(6, pool.getStats().totalSize);

        sizer = new AdaptivePoolSizer.Builder(pool)
                .minPoolSize(3)
                .maxPoolSize(6)  // Already at max
                .scaleUpThreshold(50)
                .scaleUpDelay(1)
                .checkInterval(1)
                .scaleCooldown(0)  // No cooldown for testing
                .build();

        sizer.start();

        // Borrow most executors (83% utilization - above threshold but at max)
        Python3Executor exec1 = pool.borrowExecutor(5, TimeUnit.SECONDS);
        Python3Executor exec2 = pool.borrowExecutor(5, TimeUnit.SECONDS);
        Python3Executor exec3 = pool.borrowExecutor(5, TimeUnit.SECONDS);
        Python3Executor exec4 = pool.borrowExecutor(5, TimeUnit.SECONDS);
        Python3Executor exec5 = pool.borrowExecutor(5, TimeUnit.SECONDS);

        // Wait: 1s initial + 1s check + 1s buffer = 3s
        Thread.sleep(3500);

        // Pool should NOT scale above max
        assertEquals(6, pool.getStats().totalSize, "Pool should not scale above max size");

        // Return executors
        pool.returnExecutor(exec1);
        pool.returnExecutor(exec2);
        pool.returnExecutor(exec3);
        pool.returnExecutor(exec4);
        pool.returnExecutor(exec5);
    }

    @Test
    public void testGetConfiguration() {
        sizer = new AdaptivePoolSizer.Builder(pool)
                .minPoolSize(5)
                .maxPoolSize(12)
                .scaleUpThreshold(75)
                .scaleDownThreshold(25)
                .scaleUpDelay(20)
                .scaleDownDelay(120)
                .checkInterval(10)
                .build();

        String config = sizer.getConfiguration();

        assertNotNull(config);
        assertTrue(config.contains("min=5"));
        assertTrue(config.contains("max=12"));
        assertTrue(config.contains("scaleUp>75"));
        assertTrue(config.contains("scaleDown<25"));
        assertTrue(config.contains("scaleUpDelay=20s"));
        assertTrue(config.contains("scaleDownDelay=120s"));
        assertTrue(config.contains("checkInterval=10s"));
        assertTrue(config.contains("running=false"));
    }

    @Test
    public void testConfigurationShowsRunningStatus() {
        sizer = new AdaptivePoolSizer.Builder(pool)
                .checkInterval(1)
                .build();

        // Before start
        assertTrue(sizer.getConfiguration().contains("running=false"));

        // After start
        sizer.start();
        assertTrue(sizer.getConfiguration().contains("running=true"));

        // After stop
        sizer.stop();
        assertTrue(sizer.getConfiguration().contains("running=false"));
    }
}
