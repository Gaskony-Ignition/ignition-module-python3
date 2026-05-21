package com.gaskony.python3.gateway;

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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.atomic.AtomicBoolean;

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

    private static final Logger LEGACY_LOGGER =
        LoggerFactory.getLogger(ExecutionPriority.class);

    /**
     * One-shot latch for the legacy {@code "RESTRICTED"} priority warning.
     * @see #fromString(String)
     */
    private static final AtomicBoolean LEGACY_RESTRICTED_WARNED =
        new AtomicBoolean(false);

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
     * <p>v4.0.0: the legacy {@code "RESTRICTED"} value (previously used as a
     * non-NORMAL priority before the C13 sandbox cleanup) is detected and
     * emits a one-time {@code WARN}. It maps to {@link #NORMAL} so existing
     * callers don't break, but the warning surfaces the legacy usage so
     * operators can update callers before {@code "RESTRICTED"} is rejected
     * outright in a future major.</p>
     *
     * @param priority Priority string ("high", "normal", "low")
     * @return ExecutionPriority, or NORMAL if invalid
     */
    public static ExecutionPriority fromString(String priority) {
        if (priority == null) {
            return NORMAL;
        }

        if ("restricted".equalsIgnoreCase(priority)
                && LEGACY_RESTRICTED_WARNED.compareAndSet(false, true)) {
            LEGACY_LOGGER.warn(
                "Received legacy priority=\"RESTRICTED\" wire value. This "
                + "priority was removed in v4.0.0 alongside the RESTRICTED "
                + "security mode and now maps to NORMAL. Update the caller "
                + "to pass \"normal\", \"high\", or \"low\" — the literal "
                + "string \"RESTRICTED\" is scheduled for explicit rejection "
                + "in a future major. This warning is emitted once per JVM."
            );
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
