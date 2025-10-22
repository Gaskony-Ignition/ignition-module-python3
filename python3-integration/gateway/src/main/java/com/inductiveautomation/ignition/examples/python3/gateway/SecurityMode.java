package com.inductiveautomation.ignition.examples.python3.gateway;

/**
 * Security modes for Python code execution.
 * Determines which Python modules and functions are allowed.
 *
 * @since v2.6.0
 */
public enum SecurityMode {
    /**
     * DESIGNER_ADMIN: Full Python capabilities for Designer IDE users.
     * <p>
     * Intended for: Authenticated Designer users (developers)
     * Trust Level: HIGH - Users already have Designer access
     * <p>
     * Capabilities:
     * - All modules allowed except 'always_blocked' (ctypes, multiprocessing, etc.)
     * - Can import: os, sys, subprocess, requests, pandas, numpy, etc.
     * - Can execute shell commands
     * - Can perform file I/O
     * - Can make network requests
     * <p>
     * Restrictions:
     * - Dangerous modules blocked: ctypes, multiprocessing, telnetlib, etc.
     * - Resource limits: 512MB RAM, 60s CPU time
     * - Audit logging: All actions logged
     * <p>
     * Auto-detected for Designer IDE requests via User-Agent header.
     */
    DESIGNER_ADMIN("DESIGNER_ADMIN"),

    /**
     * ADMIN: Extended capabilities for authenticated administrators.
     * <p>
     * Intended for: REST API users with admin API key, Ignition Administrator role users
     * Trust Level: MEDIUM - Authenticated with admin credentials
     * <p>
     * Capabilities:
     * - safe_modules + admin_modules allowed
     * - Can import: os, sys, subprocess, requests, pandas, sqlite3, etc.
     * - Cannot import dangerous modules: ctypes, multiprocessing, etc.
     * <p>
     * Requirements:
     * - REST API: Requires admin API key (32+ chars) via Authorization header
     * - REST API: Requires HTTPS (SSL/TLS)
     * - Gateway Scripts: Requires Ignition Administrator role
     * <p>
     * Security:
     * - Rate limiting: 100 requests/minute
     * - Resource limits: 512MB RAM, 60s CPU time
     * - Audit logging: All actions logged
     */
    ADMIN("ADMIN"),

    /**
     * RESTRICTED: Safe modules only (default security mode).
     * <p>
     * Intended for: Regular users, unauthenticated REST API access
     * Trust Level: LOW - Untrusted or regular users
     * <p>
     * Capabilities:
     * - Safe modules only: math, json, datetime, itertools, random, etc.
     * - Cannot import: os, sys, subprocess, requests, file I/O modules
     * - Cannot use dangerous functions: eval, exec, __import__, open, etc.
     * <p>
     * Use Cases:
     * - Data processing (calculations, transformations)
     * - JSON/CSV parsing
     * - Date/time operations
     * - Mathematical computations
     * <p>
     * Security:
     * - Strict module whitelist
     * - AST-based validation (prevents bypass)
     * - Rate limiting: 100 requests/minute
     * - Resource limits: 512MB RAM, 60s CPU time
     * - Audit logging: All actions logged
     */
    RESTRICTED("RESTRICTED");

    private final String value;

    SecurityMode(String value) {
        this.value = value;
    }

    /**
     * Get the string value of this security mode.
     * Used for communication with Python bridge.
     *
     * @return The string representation (e.g., "DESIGNER_ADMIN")
     */
    public String getValue() {
        return value;
    }

    /**
     * Parse a security mode from string value.
     * Case-insensitive matching.
     *
     * @param value The string value to parse
     * @return The corresponding SecurityMode, or RESTRICTED if not recognized (safe default)
     */
    public static SecurityMode fromString(String value) {
        if (value == null) {
            return RESTRICTED;
        }

        for (SecurityMode mode : values()) {
            if (mode.value.equalsIgnoreCase(value)) {
                return mode;
            }
        }

        // Safe default - if mode not recognized, use most restrictive
        return RESTRICTED;
    }

    /**
     * Check if this mode allows admin-level operations.
     *
     * @return true if DESIGNER_ADMIN or ADMIN mode
     */
    public boolean isAdminMode() {
        return this == DESIGNER_ADMIN || this == ADMIN;
    }

    /**
     * Check if this mode allows full capabilities (Designer users).
     *
     * @return true if DESIGNER_ADMIN mode
     */
    public boolean isDesignerMode() {
        return this == DESIGNER_ADMIN;
    }

    /**
     * Check if this mode is restricted (safe modules only).
     *
     * @return true if RESTRICTED mode
     */
    public boolean isRestricted() {
        return this == RESTRICTED;
    }

    @Override
    public String toString() {
        return value;
    }
}
