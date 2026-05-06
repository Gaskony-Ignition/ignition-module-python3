package com.inductiveautomation.ignition.examples.python3.gateway;

/**
 * Execution Priority Levels.
 *
 * <p>Defines priority levels for Python execution requests.
 * Higher priority requests are executed before lower priority requests when the pool is busy.
 *
 * <h2>Priority Levels:</h2>
 * <ul>
 *   <li><b>HIGH:</b> Admin users, critical operations, interactive requests</li>
 *   <li><b>NORMAL:</b> Regular user requests (default)</li>
 *   <li><b>LOW:</b> Background tasks, batch operations, non-critical queries</li>
 * </ul>
 *
 * @since v2.16.0 (Phase 3 Week 3-4: Performance Optimization)
 */
public enum ExecutionPriority {
    /**
     * High priority - executed first.
     * Use for admin users, critical operations, interactive requests.
     */
    HIGH(1),

    /**
     * Normal priority - default.
     * Use for regular user requests.
     */
    NORMAL(5),

    /**
     * Low priority - executed last.
     * Use for background tasks, batch operations, non-critical queries.
     */
    LOW(10);

    private final int value;

    ExecutionPriority(int value) {
        this.value = value;
    }

    /**
     * Get numeric priority value (lower = higher priority).
     *
     * @return Priority value
     */
    public int getValue() {
        return value;
    }

    /**
     * Parse priority from string (case-insensitive).
     *
     * @param priority Priority string ("high", "normal", "low")
     * @return ExecutionPriority, or NORMAL if invalid
     */
    public static ExecutionPriority fromString(String priority) {
        if (priority == null) {
            return NORMAL;
        }

        switch (priority.toLowerCase()) {
            case "high":
                return HIGH;
            case "low":
                return LOW;
            case "normal":
            default:
                return NORMAL;
        }
    }

    /**
     * Get priority based on security mode.
     * Admin users get HIGH priority, others get NORMAL.
     *
     * @param securityMode Security mode
     * @return ExecutionPriority
     */
    public static ExecutionPriority fromSecurityMode(SecurityMode securityMode) {
        if (securityMode == null) {
            return NORMAL;
        }

        switch (securityMode) {
            case DESIGNER_ADMIN:
            case ADMIN:
                return HIGH;
            default:
                // C13: RESTRICTED was removed; this branch retained for any
                // future SecurityMode value that should not get HIGH priority.
                return NORMAL;
        }
    }
}
