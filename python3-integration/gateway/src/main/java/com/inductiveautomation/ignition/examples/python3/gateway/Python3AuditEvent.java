package com.inductiveautomation.ignition.examples.python3.gateway;

import java.time.Instant;
import java.util.Objects;

/**
 * Audit event data for Python code execution.
 * Immutable record of a single Python execution for compliance and security monitoring.
 *
 * @since v2.6.0
 */
public class Python3AuditEvent {
    private final Instant timestamp;
    private final String user;
    private final String sourceIP;
    private final SecurityMode securityMode;
    private final String codeHash;
    private final boolean success;
    private final long durationMs;
    private final String errorMessage;
    private final String endpoint;

    /**
     * Create a new audit event for a Python execution.
     *
     * @param timestamp The time the execution occurred
     * @param user The username who executed the code (null if unauthenticated)
     * @param sourceIP The source IP address
     * @param securityMode The security mode used (DESIGNER_ADMIN, ADMIN, RESTRICTED)
     * @param codeHash SHA-256 hash of the executed code
     * @param success Whether the execution succeeded
     * @param durationMs Execution duration in milliseconds
     * @param errorMessage Error message if execution failed (null if success)
     * @param endpoint The endpoint that triggered execution (e.g., "REST:/api/v1/exec", "SCRIPT:system.python3.exec")
     */
    public Python3AuditEvent(Instant timestamp, String user, String sourceIP, SecurityMode securityMode,
                             String codeHash, boolean success, long durationMs, String errorMessage,
                             String endpoint) {
        this.timestamp = Objects.requireNonNull(timestamp, "Timestamp cannot be null");
        this.user = user; // Can be null for unauthenticated
        this.sourceIP = sourceIP; // Can be null for local script console
        this.securityMode = Objects.requireNonNull(securityMode, "Security mode cannot be null");
        this.codeHash = Objects.requireNonNull(codeHash, "Code hash cannot be null");
        this.success = success;
        this.durationMs = durationMs;
        this.errorMessage = errorMessage; // Can be null if success
        this.endpoint = Objects.requireNonNull(endpoint, "Endpoint cannot be null");
    }

    // Getters
    public Instant getTimestamp() {
        return timestamp;
    }

    public String getUser() {
        return user;
    }

    public String getSourceIP() {
        return sourceIP;
    }

    public SecurityMode getSecurityMode() {
        return securityMode;
    }

    public String getCodeHash() {
        return codeHash;
    }

    public boolean isSuccess() {
        return success;
    }

    public long getDurationMs() {
        return durationMs;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public String getEndpoint() {
        return endpoint;
    }

    /**
     * Check if this event represents an admin-level operation.
     * @return true if security mode is DESIGNER_ADMIN or ADMIN
     */
    public boolean isAdminOperation() {
        return securityMode.isAdminMode();
    }

    /**
     * Check if this event represents a Designer IDE operation.
     * @return true if security mode is DESIGNER_ADMIN
     */
    public boolean isDesignerOperation() {
        return securityMode.isDesignerMode();
    }

    /**
     * Get a single-line summary of this audit event for logging.
     * @return Human-readable audit log line
     */
    public String toLogLine() {
        return String.format("AUDIT: timestamp=%s, user=%s, ip=%s, mode=%s, codeHash=%s, success=%s, duration=%dms, endpoint=%s%s",
            timestamp,
            user != null ? user : "UNAUTHENTICATED",
            sourceIP != null ? sourceIP : "LOCAL",
            securityMode,
            codeHash.substring(0, Math.min(12, codeHash.length())), // First 12 chars of hash
            success,
            durationMs,
            endpoint,
            errorMessage != null ? ", error=" + errorMessage : ""
        );
    }

    @Override
    public String toString() {
        return toLogLine();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Python3AuditEvent that = (Python3AuditEvent) o;
        return success == that.success &&
            durationMs == that.durationMs &&
            Objects.equals(timestamp, that.timestamp) &&
            Objects.equals(user, that.user) &&
            Objects.equals(sourceIP, that.sourceIP) &&
            securityMode == that.securityMode &&
            Objects.equals(codeHash, that.codeHash) &&
            Objects.equals(errorMessage, that.errorMessage) &&
            Objects.equals(endpoint, that.endpoint);
    }

    @Override
    public int hashCode() {
        return Objects.hash(timestamp, user, sourceIP, securityMode, codeHash, success, durationMs, errorMessage, endpoint);
    }
}
