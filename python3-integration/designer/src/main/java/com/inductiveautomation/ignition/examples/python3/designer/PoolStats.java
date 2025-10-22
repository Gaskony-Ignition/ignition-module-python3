package com.inductiveautomation.ignition.examples.python3.designer;

/**
 * Represents Python process pool statistics from the REST API.
 *
 * <p>Example JSON response from /api/v1/pool-stats:</p>
 * <pre>{
 *   "totalSize": 3,
 *   "healthy": 3,
 *   "available": 2,
 *   "inUse": 1
 * }</pre>
 */
public class PoolStats {
    private final int totalSize;
    private final int healthy;
    private final int available;
    private final int inUse;

    public PoolStats(int totalSize, int healthy, int available, int inUse) {
        this.totalSize = totalSize;
        this.healthy = healthy;
        this.available = available;
        this.inUse = inUse;
    }

    public int getTotalSize() {
        return totalSize;
    }

    public int getHealthy() {
        return healthy;
    }

    public int getAvailable() {
        return available;
    }

    public int getInUse() {
        return inUse;
    }

    public boolean isHealthy() {
        return healthy == totalSize;
    }

    @Override
    public String toString() {
        return String.format("PoolStats{total=%d, healthy=%d, available=%d, inUse=%d}",
                totalSize, healthy, available, inUse);
    }
}
