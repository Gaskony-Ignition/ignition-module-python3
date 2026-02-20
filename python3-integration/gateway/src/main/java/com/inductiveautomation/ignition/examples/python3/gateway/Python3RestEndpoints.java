package com.inductiveautomation.ignition.examples.python3.gateway;

import com.inductiveautomation.ignition.common.gson.JsonArray;
import com.inductiveautomation.ignition.common.gson.JsonElement;
import com.inductiveautomation.ignition.common.gson.JsonObject;
import com.inductiveautomation.ignition.common.gson.JsonParser;
import com.inductiveautomation.ignition.gateway.dataroutes.HttpMethod;
import com.inductiveautomation.ignition.gateway.dataroutes.RequestContext;
import com.inductiveautomation.ignition.gateway.dataroutes.RouteGroup;
import com.inductiveautomation.ignition.gateway.dataroutes.RouteAccess;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * REST API endpoints for Python 3 Integration module.
 * Provides HTTP access to all Python3 functions via JSON API.
 *
 * Endpoints are mounted at: http://host:port/main/data/python3integration/
 */
public final class Python3RestEndpoints {

    private static final Logger LOGGER = LoggerFactory.getLogger(Python3RestEndpoints.class);
    private static Python3ScriptModule scriptModule;
    private static Python3ScriptRepository scriptRepository;
    private static Python3MetricsCollector metricsCollector = new Python3MetricsCollector();
    private static Python3PackageManager packageManager;
    private static Python3SecurityService securityService;
    private static Python3AuditLogger auditLogger;
    private static PoolManager poolManager;
    private static PythonDistributionManager distributionManager;
    private static java.io.File logsDir;

    // Security: Rate limiting (100 requests per minute per user)
    private static final int RATE_LIMIT_PER_MINUTE = 100;
    private static final int MAX_RATE_LIMITERS = 10_000;  // v2.15.9: Prevent memory leak
    private static final Map<String, RateLimiter> userRateLimiters = new ConcurrentHashMap<>();

    // Security: Audit logging enabled
    private static final boolean AUDIT_LOGGING_ENABLED = true;

    // Security: Input validation limits
    private static final int MAX_CODE_SIZE = 1_048_576;  // 1MB
    private static final int MAX_SCRIPT_NAME_LENGTH = 255;
    private static final int MAX_FOLDER_PATH_LENGTH = 1000;

    // Security: IP whitelisting for ADMIN mode (v2.15.9)
    private static final String IP_WHITELIST_PROPERTY = "ignition.python3.admin.ip.whitelist";
    private static Set<String> allowedIPs = Collections.emptySet();
    private static boolean ipWhitelistEnabled = false;

    private Python3RestEndpoints() {
        // Private constructor for utility class
    }

    /**
     * Initialize IP whitelist from system property.
     * Called once during module startup.
     *
     * Format: comma-separated list of IPs or CIDR ranges
     * Example: "192.168.1.100,10.0.0.0/8,172.16.0.0/12"
     *
     * If not set or empty, all IPs are allowed (backward compatible).
     *
     * v2.15.9: IP whitelisting for production security
     */
    private static void loadIPWhitelist() {
        String whitelist = System.getProperty(IP_WHITELIST_PROPERTY);

        if (whitelist == null || whitelist.trim().isEmpty()) {
            LOGGER.info("IP whitelist not configured - all IPs allowed for ADMIN mode");
            ipWhitelistEnabled = false;
            allowedIPs = Collections.emptySet();
            return;
        }

        // Parse comma-separated IPs/CIDR ranges
        Set<String> ips = ConcurrentHashMap.newKeySet();
        for (String ip : whitelist.split(",")) {
            String trimmed = ip.trim();
            if (!trimmed.isEmpty()) {
                ips.add(trimmed);
                LOGGER.info("Added IP to whitelist: {}", trimmed);
            }
        }

        if (ips.isEmpty()) {
            LOGGER.warn("IP whitelist configured but empty - all IPs allowed");
            ipWhitelistEnabled = false;
            allowedIPs = Collections.emptySet();
        } else {
            allowedIPs = ips;
            ipWhitelistEnabled = true;
            LOGGER.info("IP whitelist enabled with {} entries", ips.size());
            LOGGER.warn("ADMIN mode access restricted to whitelisted IPs only");
        }
    }

    /**
     * Validate request IP against whitelist (if enabled).
     * Only enforced for ADMIN mode requests.
     *
     * @param req The request context
     * @param securityMode The security mode for this request
     * @throws SecurityException if IP is not whitelisted for ADMIN mode
     */
    private static void validateIPWhitelist(RequestContext req, SecurityMode securityMode) throws SecurityException {
        // Only enforce for ADMIN mode (DESIGNER_ADMIN and RESTRICTED are allowed from any IP)
        if (!ipWhitelistEnabled || securityMode != SecurityMode.ADMIN) {
            return;
        }

        String clientIP = getClientIPAddress(req);

        if (!isIPAllowed(clientIP)) {
            LOGGER.warn("SECURITY: ADMIN mode request rejected from non-whitelisted IP: {}", clientIP);
            throw new SecurityException(
                "Access denied: Your IP address (" + clientIP + ") is not whitelisted for ADMIN mode. " +
                "Contact your administrator to add your IP to: " + IP_WHITELIST_PROPERTY
            );
        }

        LOGGER.debug("IP whitelist check passed for: {}", clientIP);
    }

    /**
     * Check if an IP address is allowed by the whitelist.
     * Supports individual IPs and CIDR ranges.
     *
     * @param clientIP The client IP address
     * @return true if allowed (or whitelist disabled)
     */
    private static boolean isIPAllowed(String clientIP) {
        if (!ipWhitelistEnabled || clientIP == null) {
            return true;  // Whitelist disabled or no IP
        }

        // Direct IP match
        if (allowedIPs.contains(clientIP)) {
            return true;
        }

        // CIDR range match
        for (String allowedEntry : allowedIPs) {
            if (allowedEntry.contains("/")) {
                // CIDR notation - check if IP is in range
                if (isIPInCIDR(clientIP, allowedEntry)) {
                    return true;
                }
            }
        }

        return false;
    }

    /**
     * Check if an IP address is within a CIDR range.
     * Simple implementation for IPv4.
     *
     * @param ip The IP address to check (e.g., "192.168.1.100")
     * @param cidr The CIDR range (e.g., "192.168.1.0/24")
     * @return true if IP is in range
     */
    private static boolean isIPInCIDR(String ip, String cidr) {
        try {
            String[] cidrParts = cidr.split("/");
            if (cidrParts.length != 2) {
                return false;
            }

            String networkIP = cidrParts[0];
            int prefixLength = Integer.parseInt(cidrParts[1]);

            // Convert IPs to integers for comparison
            long ipLong = ipToLong(ip);
            long networkLong = ipToLong(networkIP);

            // Calculate network mask
            long mask = ((1L << prefixLength) - 1) << (32 - prefixLength);

            // Check if IP is in network range
            return (ipLong & mask) == (networkLong & mask);

        } catch (Exception e) {
            LOGGER.warn("Error parsing CIDR range: {} - {}", cidr, e.getMessage());
            return false;
        }
    }

    /**
     * Convert IPv4 address string to long integer.
     *
     * @param ip IPv4 address (e.g., "192.168.1.100")
     * @return Long representation
     */
    private static long ipToLong(String ip) {
        String[] octets = ip.split("\\.");
        if (octets.length != 4) {
            throw new IllegalArgumentException("Invalid IPv4 address: " + ip);
        }

        long result = 0;
        for (int i = 0; i < 4; i++) {
            int octet = Integer.parseInt(octets[i]);
            if (octet < 0 || octet > 255) {
                throw new IllegalArgumentException("Invalid IPv4 octet: " + octet);
            }
            result = (result << 8) | octet;
        }
        return result;
    }

    /**
     * Get client IP address from request, handling proxies.
     * Checks X-Forwarded-For header first, then falls back to remote address.
     *
     * @param req The request context
     * @return Client IP address
     */
    private static String getClientIPAddress(RequestContext req) {
        HttpServletRequest httpReq = req.getRequest();

        // Check X-Forwarded-For header (for proxies/load balancers)
        String xForwardedFor = httpReq.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.trim().isEmpty()) {
            // X-Forwarded-For can contain multiple IPs, use the first (client)
            String[] ips = xForwardedFor.split(",");
            return ips[0].trim();
        }

        // Fall back to remote address
        return httpReq.getRemoteAddr();
    }

    /**
     * Simple rate limiter to prevent abuse
     */
    private static class RateLimiter {
        private final AtomicInteger requestCount = new AtomicInteger(0);
        private long windowStart = System.currentTimeMillis();
        private static final long WINDOW_MS = 60000; // 1 minute

        public synchronized boolean allowRequest() {
            long now = System.currentTimeMillis();

            // Reset window if expired
            if (now - windowStart > WINDOW_MS) {
                requestCount.set(0);
                windowStart = now;
            }

            int count = requestCount.incrementAndGet();
            return count <= RATE_LIMIT_PER_MINUTE;
        }

        public synchronized int getRemainingRequests() {
            return Math.max(0, RATE_LIMIT_PER_MINUTE - requestCount.get());
        }
    }

    /**
     * Determine security mode for the current request.
     * This is used by execution handlers to determine what Python modules are allowed.
     *
     * Security Model (v2.6.0):
     * - DESIGNER_ADMIN: Designer IDE users (full Python capabilities)
     * - ADMIN: REST API with admin key (extended capabilities)
     * - RESTRICTED: Unauthenticated REST API (safe modules only)
     *
     * @param req The request context
     * @return The security mode for this request
     * @since v2.6.0
     */
    private static SecurityMode determineSecurityMode(RequestContext req) {
        if (securityService == null) {
            LOGGER.warn("Security service not initialized - defaulting to RESTRICTED mode");
            return SecurityMode.RESTRICTED;
        }

        try {
            SecurityMode mode = securityService.determineSecurityMode(req);

            // Enforce HTTPS requirement for ADMIN mode
            securityService.enforceHttpsRequirement(mode, req);

            // v2.15.9: Enforce IP whitelist for ADMIN mode
            validateIPWhitelist(req, mode);

            return mode;
        } catch (SecurityException e) {
            LOGGER.warn("Security check failed: {}", e.getMessage());
            return SecurityMode.RESTRICTED;
        }
    }

    /**
     * Check if user has permission to execute Python code.
     *
     * Security Model (v2.6.0):
     * - All requests are allowed (access control is handled via security modes)
     * - DESIGNER_ADMIN: Designer IDE users (trusted, full access)
     * - ADMIN: REST API with admin key (extended access)
     * - RESTRICTED: Unauthenticated REST API (safe modules only)
     *
     * Rate limiting is still enforced to prevent abuse.
     *
     * @param req The request context
     * @return GRANTED (security enforced via security modes, not access control)
     */
    private static RouteAccess checkExecutePermission(RequestContext req) {
        // v3.5.2: Require Gateway authentication
        if (!isGatewayAuthenticated(req)) {
            return RouteAccess.UNAUTHORIZED;
        }

        // Rate limiting still applies
        String clientId = getClientId(req);
        RateLimiter limiter = userRateLimiters.computeIfAbsent(clientId, k -> {
            // v2.15.9: Prevent unbounded growth - clear oldest entries if limit reached
            if (userRateLimiters.size() >= MAX_RATE_LIMITERS) {
                LOGGER.warn("Rate limiter map size limit reached ({}), clearing half", MAX_RATE_LIMITERS);
                // Remove first half of entries (oldest in iteration order)
                int toRemove = MAX_RATE_LIMITERS / 2;
                userRateLimiters.keySet().stream().limit(toRemove).forEach(userRateLimiters::remove);
            }
            return new RateLimiter();
        });

        if (!limiter.allowRequest()) {
            LOGGER.warn("Rate limit exceeded for client: {}", clientId);
            // Note: RouteAccess doesn't have DENIED - throw exception to prevent access
            throw new SecurityException("Rate limit exceeded. Maximum " + RATE_LIMIT_PER_MINUTE + " requests per minute.");
        }

        return RouteAccess.GRANTED;
    }

    /**
     * Get a client identifier for rate limiting.
     * Uses IP address as identifier.
     *
     * @param req The request context
     * @return Client identifier
     */
    private static String getClientId(RequestContext req) {
        String remoteAddr = req.getRequest().getRemoteAddr();
        return remoteAddr != null ? remoteAddr : "unknown";
    }

    /**
     * Check if user is authenticated via Gateway session or Bearer token.
     *
     * Accepts:
     * 1. Gateway HTTP session with "user" attribute (browser-based login)
     * 2. Valid Bearer token from Authorization header (Designer REST client)
     * 3. Valid actor from request context
     *
     * v3.5.2: Require authentication for all endpoints
     */
    private static boolean isGatewayAuthenticated(RequestContext req) {
        try {
            // 1. Check HTTP session for "user" attribute (set by Gateway on login)
            var httpSession = req.getRequest().getSession(false);
            if (httpSession != null) {
                var authUser = httpSession.getAttribute("user");
                if (authUser != null) {
                    return true;
                }
            }
            // 2. Check Bearer token (Designer REST client uses this)
            String authHeader = req.getRequest().getHeader("Authorization");
            if (authHeader != null && authHeader.startsWith("Bearer ") && securityService != null) {
                try {
                    securityService.determineSecurityMode(req);
                    // If no exception thrown, token is valid
                    return true;
                } catch (SecurityException e) {
                    LOGGER.debug("Bearer token validation failed: {}", e.getMessage());
                }
            }
            // 3. Check actor from request context (alternative auth path)
            var actor = req.getActor();
            if (actor != null && !actor.isEmpty() && !"unknown".equalsIgnoreCase(actor)) {
                return true;
            }
        } catch (Exception e) {
            LOGGER.debug("Error checking gateway authentication", e);
        }
        return false;
    }

    /**
     * Check if user has permission to manage scripts (save, delete).
     * Requires Gateway authentication.
     *
     * v3.5.2: Require Gateway login
     */
    private static RouteAccess checkManagePermission(RequestContext req) {
        return isGatewayAuthenticated(req) ? RouteAccess.GRANTED : RouteAccess.UNAUTHORIZED;
    }

    /**
     * Check if user has permission to read data (diagnostics, stats).
     * Requires Gateway authentication.
     *
     * v3.5.2: Require Gateway login
     */
    private static RouteAccess checkReadPermission(RequestContext req) {
        return isGatewayAuthenticated(req) ? RouteAccess.GRANTED : RouteAccess.UNAUTHORIZED;
    }

    // NOTE: Legacy getSecurityMode removed in v2.6.0
    // Use determineSecurityMode() which returns SecurityMode enum

    /**
     * Constant-time string comparison to prevent timing attacks.
     * Used for comparing API keys and sensitive tokens.
     *
     * v1.17.0: Security enhancement - prevents timing-based credential enumeration
     * v2.15.9: Fixed timing leak in length comparison
     */
    private static boolean secureEquals(String a, String b) {
        if (a == null || b == null) {
            return a == b;
        }

        // Constant-time length comparison (v2.15.9 fix)
        int lengthA = a.length();
        int lengthB = b.length();
        int result = lengthA ^ lengthB;  // Will be non-zero if lengths differ

        // Always compare up to the minimum length to avoid timing leaks
        int minLength = Math.min(lengthA, lengthB);
        for (int i = 0; i < minLength; i++) {
            result |= a.charAt(i) ^ b.charAt(i);
        }

        // XOR with dummy value for remaining bytes if lengths differ
        // This ensures timing is constant regardless of length difference
        for (int i = minLength; i < Math.max(lengthA, lengthB); i++) {
            result |= 0xFF;  // Ensure result is non-zero if lengths differ
        }

        return result == 0;
    }

    /**
     * Apply comprehensive security headers to HTTP responses.
     * Provides defense-in-depth against common web vulnerabilities.
     *
     * v1.17.0: Implemented security headers (CSP, HSTS, X-Frame-Options, etc.)
     */
    private static void applySecurityHeaders(HttpServletResponse res) {
        // Content Security Policy - prevent XSS attacks
        res.setHeader("Content-Security-Policy",
            "default-src 'self'; script-src 'none'; object-src 'none'; base-uri 'self'; form-action 'self'");

        // HTTP Strict Transport Security - force HTTPS
        res.setHeader("Strict-Transport-Security",
            "max-age=31536000; includeSubDomains");

        // Prevent clickjacking
        res.setHeader("X-Frame-Options", "DENY");

        // Prevent MIME sniffing
        res.setHeader("X-Content-Type-Options", "nosniff");

        // XSS protection (legacy, but still useful)
        res.setHeader("X-XSS-Protection", "1; mode=block");

        // Referrer policy - don't leak information
        res.setHeader("Referrer-Policy", "no-referrer");

        // Permissions policy - disable unnecessary features
        res.setHeader("Permissions-Policy",
            "geolocation=(), microphone=(), camera=(), payment=()");
    }

    // CSRF Protection (v1.17.0)
    // Fixed memory leak in v2.15.9: Added timestamp tracking and cleanup
    private static final Map<String, String> csrfTokens = new ConcurrentHashMap<>();
    private static final Map<String, Long> csrfTokenTimestamps = new ConcurrentHashMap<>();
    private static final long CSRF_TOKEN_EXPIRY_MS = 28800000; // 8 hours (match session token expiry)

    /**
     * Generate CSRF token for a session.
     *
     * v1.17.0: CSRF protection for state-changing operations
     * v2.15.9: Added timestamp tracking and lazy cleanup to prevent memory leak
     */
    private static String generateCSRFToken(String sessionId) {
        // Lazy cleanup of expired tokens
        cleanupExpiredCSRFTokens();

        String token = java.util.UUID.randomUUID().toString();
        csrfTokens.put(sessionId, token);
        csrfTokenTimestamps.put(sessionId, System.currentTimeMillis());
        return token;
    }

    /**
     * Cleanup expired CSRF tokens to prevent memory leak.
     * v2.15.9: Memory leak fix
     */
    private static void cleanupExpiredCSRFTokens() {
        long now = System.currentTimeMillis();
        csrfTokenTimestamps.entrySet().removeIf(entry -> {
            if (now - entry.getValue() > CSRF_TOKEN_EXPIRY_MS) {
                csrfTokens.remove(entry.getKey());
                return true;
            }
            return false;
        });
    }

    /**
     * Validate CSRF token for state-changing operations.
     *
     * v1.17.0: CSRF protection
     * v2.15.9: Now called from all state-changing endpoints
     */
    private static boolean validateCSRFToken(RequestContext req) {
        try {
            String sessionId = req.getRequest().getSession(false) != null ?
                    req.getRequest().getSession(false).getId() : null;

            if (sessionId == null) {
                LOGGER.warn("CSRF validation failed: no session");
                return false;
            }

            String providedToken = req.getRequest().getHeader("X-CSRF-Token");
            if (providedToken == null || providedToken.trim().isEmpty()) {
                LOGGER.warn("CSRF validation failed: no token provided");
                return false;
            }

            String expectedToken = csrfTokens.get(sessionId);
            if (expectedToken == null) {
                LOGGER.warn("CSRF validation failed: no token for session");
                return false;
            }

            boolean valid = secureEquals(providedToken, expectedToken);
            if (!valid) {
                LOGGER.warn("CSRF validation failed: token mismatch");
            }

            return valid;

        } catch (Exception e) {
            LOGGER.error("CSRF validation error", e);
            return false;
        }
    }

    /**
     * Validate CSRF token if request has a session.
     * For browser-based requests (Gateway Web UI), CSRF protection is required.
     * For Bearer token requests (Designer REST client), CSRF validation is skipped.
     *
     * v2.15.9: Added for conditional CSRF validation
     * v3.5.3: Skip CSRF for requests with valid Bearer token (Designer client)
     */
    private static void validateCSRFIfSession(RequestContext req) {
        // Skip CSRF for requests authenticated via Bearer token (Designer REST client)
        String authHeader = req.getRequest().getHeader("Authorization");
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            return;
        }

        // Require CSRF for browser-based session requests (Gateway Web UI)
        if (req.getRequest().getSession(false) != null) {
            if (!validateCSRFToken(req)) {
                throw new SecurityException("CSRF token validation failed. Include X-CSRF-Token header.");
            }
        }
    }

    /**
     * Audit log Python code execution (simplified - no user context available in SDK)
     */
    private static void auditLog(String action, String details) {
        if (!AUDIT_LOGGING_ENABLED) {
            return;
        }

        try {
            // Log to Gateway logs (in production, also log to database)
            LOGGER.info("AUDIT: Action={}, Details={}",
                action, sanitizeForLogging(details));

            // NOTE: Database audit logging planned for future roadmap (see docs/roadmap/PYTHON_SANDBOXING_AND_SECURITY.md)
            // For now, log file auditing is sufficient for most use cases
            // Future: scriptRepository.writeAuditLog(action, hashCode(details));

        } catch (Exception e) {
            LOGGER.error("Failed to write audit log", e);
        }
    }

    /**
     * Sanitize data for logging (truncate, redact secrets)
     */
    private static String sanitizeForLogging(String data) {
        if (data == null) {
            return "";
        }

        // Redact potential secrets
        String sanitized = data;
        sanitized = sanitized.replaceAll("password\\s*=\\s*['\"][^'\"]*['\"]", "password='***'");
        sanitized = sanitized.replaceAll("api_key\\s*=\\s*['\"][^'\"]*['\"]", "api_key='***'");
        sanitized = sanitized.replaceAll("secret\\s*=\\s*['\"][^'\"]*['\"]", "secret='***'");
        sanitized = sanitized.replaceAll("token\\s*=\\s*['\"][^'\"]*['\"]", "token='***'");

        // Truncate if too long
        if (sanitized.length() > 200) {
            sanitized = sanitized.substring(0, 200) + "... (truncated)";
        }

        return sanitized;
    }

    /**
     * Hash code for audit logging (don't store full code)
     */
    private static String hashCode(String code) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] hash = md.digest(code.getBytes("UTF-8"));
            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) {
                    hexString.append('0');
                }
                hexString.append(hex);
            }
            return hexString.toString().substring(0, 16); // First 16 chars
        } catch (Exception e) {
            return "hash_error";
        }
    }

    /**
     * Validate Python code (size limits)
     */
    private static void validateCode(String code) {
        if (code == null) {
            throw new IllegalArgumentException("Code cannot be null");
        }

        if (code.length() > MAX_CODE_SIZE) {
            throw new IllegalArgumentException(
                "Code size exceeds maximum limit of " + MAX_CODE_SIZE + " bytes (1MB). " +
                "Provided: " + code.length() + " bytes"
            );
        }

        if (code.trim().isEmpty()) {
            throw new IllegalArgumentException("Code cannot be empty");
        }
    }

    /**
     * Validate script name (alphanumeric, no SQL keywords, length limits)
     */
    private static void validateScriptName(String name) {
        if (name == null || name.trim().isEmpty()) {
            throw new IllegalArgumentException("Script name cannot be empty");
        }

        if (name.length() > MAX_SCRIPT_NAME_LENGTH) {
            throw new IllegalArgumentException(
                "Script name too long. Maximum: " + MAX_SCRIPT_NAME_LENGTH + " characters"
            );
        }

        // Allow alphanumeric, underscore, hyphen, period, space
        if (!name.matches("^[a-zA-Z0-9_. -]+$")) {
            throw new IllegalArgumentException(
                "Script name contains invalid characters. Allowed: a-z, A-Z, 0-9, _, -, ., space"
            );
        }

        // Blacklist SQL keywords (defense in depth, even though we don't use SQL)
        String upperName = name.toUpperCase();
        String[] sqlKeywords = {"SELECT", "INSERT", "UPDATE", "DELETE", "DROP", "CREATE",
                                "ALTER", "UNION", "' OR '", "1=1", "--", ";", "/*", "*/"};

        for (String keyword : sqlKeywords) {
            if (upperName.contains(keyword)) {
                throw new IllegalArgumentException(
                    "Script name contains forbidden keyword: " + keyword
                );
            }
        }
    }

    /**
     * Validate folder path (alphanumeric, no SQL keywords, length limits)
     */
    private static void validateFolderPath(String folderPath) {
        if (folderPath == null) {
            return;  // null is valid (root folder)
        }

        if (folderPath.length() > MAX_FOLDER_PATH_LENGTH) {
            throw new IllegalArgumentException(
                "Folder path too long. Maximum: " + MAX_FOLDER_PATH_LENGTH + " characters"
            );
        }

        // Allow alphanumeric, underscore, hyphen, period, forward slash, space
        if (!folderPath.matches("^[a-zA-Z0-9_./\\- ]*$")) {
            throw new IllegalArgumentException(
                "Folder path contains invalid characters. Allowed: a-z, A-Z, 0-9, _, -, ., /, space"
            );
        }

        // Blacklist SQL keywords and path traversal
        String upperPath = folderPath.toUpperCase();
        String[] forbiddenPatterns = {"SELECT", "DROP", "--", "/*", "*/", "..", "\\\\", "'", "\""};

        for (String pattern : forbiddenPatterns) {
            if (upperPath.contains(pattern) || folderPath.contains(pattern)) {
                throw new IllegalArgumentException(
                    "Folder path contains forbidden pattern: " + pattern
                );
            }
        }
    }

    /**
     * Initialize the REST endpoints with the script module instance.
     * Called from GatewayHook during startup.
     */
    public static void initialize(Python3ScriptModule module) {
        scriptModule = module;
        LOGGER.info("Python3RestEndpoints initialized");
    }

    /**
     * Set the script repository for script management endpoints.
     * Called from GatewayHook during startup.
     */
    public static void setScriptRepository(Python3ScriptRepository repository) {
        scriptRepository = repository;
        LOGGER.info("Script repository configured");
    }

    /**
     * Set the package manager for package management endpoints.
     * Called from GatewayHook during startup.
     *
     * @since v2.3.0
     */
    public static void setPackageManager(Python3PackageManager manager) {
        packageManager = manager;
        LOGGER.info("Package manager configured");
    }

    /**
     * Set the security service for authentication and authorization.
     * Called from GatewayHook during startup.
     *
     * @since v2.6.0
     */
    public static void setSecurityService(Python3SecurityService service) {
        securityService = service;
        LOGGER.info("Security service configured");
    }

    /**
     * Set the audit logger for logging executions.
     * Called from GatewayHook during startup.
     *
     * @since v2.6.0
     */
    public static void setAuditLogger(Python3AuditLogger logger) {
        auditLogger = logger;
        LOGGER.info("Audit logger configured");
    }

    /**
     * Set the process pool for subprocess monitoring.
     * Called from GatewayHook during startup.
     *
     * @since v2.15.5
     */
    public static void setProcessPool(Python3ProcessPool pool) {
        metricsCollector.setProcessPool(pool);
        LOGGER.info("Process pool configured for metrics monitoring");
    }

    /**
     * Set the pool manager for multi-version support (v3.1.0).
     */
    public static void setPoolManager(PoolManager manager) {
        poolManager = manager;
        if (manager != null) {
            LOGGER.info("Pool manager configured with {} version(s): {}",
                manager.getPoolCount(), manager.getAvailableVersions());
        }
    }

    /**
     * Set the distribution manager for Python version installation management (v3.1.0).
     */
    public static void setDistributionManager(PythonDistributionManager manager) {
        distributionManager = manager;
        if (manager != null) {
            LOGGER.info("Distribution manager configured");
        }
    }

    /**
     * Set the logs directory for reading Ignition gateway logs (v3.5.2).
     */
    public static void setLogsDir(java.io.File dir) {
        logsDir = dir;
        if (dir != null) {
            LOGGER.info("Logs directory configured: {}", dir.getAbsolutePath());
        }
    }

    /**
     * Mount all REST API routes.
     * Called from GatewayHook.mountRouteHandlers().
     *
     * Routes follow Ignition 8.3 API convention: /api/v1/{endpoint}
     * This ensures they appear in the OpenAPI specification at /openapi.json
     */
    public static void mountRoutes(RouteGroup routes) {
        LOGGER.info("Mounting Python3 REST API routes (Ignition 8.3 OpenAPI compliant)");

        // v2.15.9: Initialize IP whitelist for ADMIN mode protection
        loadIPWhitelist();

        // POST /data/python3integration/auth/session - Create session token (NEW v2.9.0)
        routes.newRoute("/auth/session")
            .handler(Python3RestEndpoints::handleCreateSession)
            .method(HttpMethod.POST)
            .type(RouteGroup.TYPE_JSON)
            .accessControl(req -> RouteAccess.GRANTED)  // No auth required to GET a token
            .mount();

        // POST /data/python3integration/api/v1/exec - Execute Python code
        routes.newRoute("/api/v1/exec")
            .handler(Python3RestEndpoints::handleExec)
            .method(HttpMethod.POST)
            .type(RouteGroup.TYPE_JSON)
            .accessControl(Python3RestEndpoints::checkExecutePermission)  // ✅ AUTH + RATE LIMIT
            .mount();

        // POST /data/python3integration/api/v1/shell-exec - Execute shell command (v2.5.0)
        routes.newRoute("/api/v1/shell-exec")
            .handler(Python3RestEndpoints::handleShellExec)
            .method(HttpMethod.POST)
            .type(RouteGroup.TYPE_JSON)
            .accessControl(Python3RestEndpoints::checkExecutePermission)  // ✅ AUTH + RATE LIMIT
            .mount();

        // POST /data/python3integration/api/v1/shell-interactive/create - Create interactive shell session (v2.5.8)
        routes.newRoute("/api/v1/shell-interactive/create")
            .handler(Python3RestEndpoints::handleCreateShellSession)
            .method(HttpMethod.POST)
            .type(RouteGroup.TYPE_JSON)
            .accessControl(Python3RestEndpoints::checkExecutePermission)  // ✅ AUTH + RATE LIMIT
            .mount();

        // POST /data/python3integration/api/v1/shell-interactive/exec - Execute command in interactive shell (v2.5.8)
        routes.newRoute("/api/v1/shell-interactive/exec")
            .handler(Python3RestEndpoints::handleInteractiveShellExec)
            .method(HttpMethod.POST)
            .type(RouteGroup.TYPE_JSON)
            .accessControl(Python3RestEndpoints::checkExecutePermission)  // ✅ AUTH + RATE LIMIT
            .mount();

        // POST /data/python3integration/api/v1/shell-interactive/close - Close interactive shell session (v2.5.8)
        routes.newRoute("/api/v1/shell-interactive/close")
            .handler(Python3RestEndpoints::handleCloseShellSession)
            .method(HttpMethod.POST)
            .type(RouteGroup.TYPE_JSON)
            .accessControl(Python3RestEndpoints::checkExecutePermission)  // ✅ AUTH + RATE LIMIT
            .mount();

        // POST /data/python3integration/api/v1/eval - Evaluate Python expression
        routes.newRoute("/api/v1/eval")
            .handler(Python3RestEndpoints::handleEval)
            .method(HttpMethod.POST)
            .type(RouteGroup.TYPE_JSON)
            .accessControl(Python3RestEndpoints::checkExecutePermission)  // ✅ AUTH + RATE LIMIT
            .mount();

        // POST /data/python3integration/api/v1/call-module - Call Python module function
        routes.newRoute("/api/v1/call-module")
            .handler(Python3RestEndpoints::handleCallModule)
            .method(HttpMethod.POST)
            .type(RouteGroup.TYPE_JSON)
            .accessControl(Python3RestEndpoints::checkExecutePermission)  // ✅ AUTH + RATE LIMIT
            .mount();

        // POST /data/python3integration/api/v1/call-script - Call saved Python script
        routes.newRoute("/api/v1/call-script")
            .handler(Python3RestEndpoints::handleCallScript)
            .method(HttpMethod.POST)
            .type(RouteGroup.TYPE_JSON)
            .accessControl(Python3RestEndpoints::checkExecutePermission)  // ✅ AUTH + RATE LIMIT
            .mount();

        // GET /data/python3integration/api/v1/version - Get Python version
        routes.newRoute("/api/v1/version")
            .handler(Python3RestEndpoints::handleGetVersion)
            .method(HttpMethod.GET)
            .type(RouteGroup.TYPE_JSON)
            .accessControl(Python3RestEndpoints::checkReadPermission)  // ✅ AUTH (read-only)
            .mount();

        // GET /data/python3integration/api/v1/pool-stats - Get process pool statistics
        routes.newRoute("/api/v1/pool-stats")
            .handler(Python3RestEndpoints::handleGetPoolStats)
            .method(HttpMethod.GET)
            .type(RouteGroup.TYPE_JSON)
            .accessControl(Python3RestEndpoints::checkReadPermission)  // ✅ AUTH (read-only)
            .mount();

        // POST /data/python3integration/api/v1/pool-size - Set process pool size (NEW v1.17.2)
        routes.newRoute("/api/v1/pool-size")
            .handler(Python3RestEndpoints::handleSetPoolSize)
            .method(HttpMethod.POST)
            .type(RouteGroup.TYPE_JSON)
            .accessControl(Python3RestEndpoints::checkManagePermission)  // ✅ AUTH (requires management permission)
            .mount();

        // GET /data/python3integration/api/v1/health - Health check
        routes.newRoute("/api/v1/health")
            .handler(Python3RestEndpoints::handleHealthCheck)
            .method(HttpMethod.GET)
            .type(RouteGroup.TYPE_JSON)
            .accessControl(Python3RestEndpoints::checkReadPermission)  // ✅ AUTH (read-only)
            .mount();

        // GET /data/python3integration/api/v1/versions - Available Python versions (v3.1.0)
        routes.newRoute("/api/v1/versions")
            .handler(Python3RestEndpoints::handleGetVersions)
            .method(HttpMethod.GET)
            .type(RouteGroup.TYPE_JSON)
            .accessControl(Python3RestEndpoints::checkReadPermission)
            .mount();

        // GET /data/python3integration/api/v1/diagnostics - Performance diagnostics
        routes.newRoute("/api/v1/diagnostics")
            .handler(Python3RestEndpoints::handleDiagnostics)
            .method(HttpMethod.GET)
            .type(RouteGroup.TYPE_JSON)
            .accessControl(Python3RestEndpoints::checkReadPermission)  // ✅ AUTH (read-only)
            .mount();

        // GET /data/python3integration/api/v1/example - Run example test
        routes.newRoute("/api/v1/example")
            .handler(Python3RestEndpoints::handleExample)
            .method(HttpMethod.GET)
            .type(RouteGroup.TYPE_JSON)
            .accessControl(Python3RestEndpoints::checkExecutePermission)  // ✅ AUTH + RATE LIMIT
            .mount();

        // POST /data/python3integration/api/v1/check-syntax - Check Python syntax
        routes.newRoute("/api/v1/check-syntax")
            .handler(Python3RestEndpoints::handleCheckSyntax)
            .method(HttpMethod.POST)
            .type(RouteGroup.TYPE_JSON)
            .accessControl(Python3RestEndpoints::checkExecutePermission)  // ✅ AUTH + RATE LIMIT
            .mount();

        // POST /data/python3integration/api/v1/completions - Get code completions
        routes.newRoute("/api/v1/completions")
            .handler(Python3RestEndpoints::handleGetCompletions)
            .method(HttpMethod.POST)
            .type(RouteGroup.TYPE_JSON)
            .accessControl(Python3RestEndpoints::checkReadPermission)  // ✅ AUTH (completions are read-only)
            .mount();

        // Performance Monitoring Endpoints

        // GET /data/python3integration/api/v1/metrics - Get performance metrics
        routes.newRoute("/api/v1/metrics")
            .handler(Python3RestEndpoints::handleGetMetrics)
            .method(HttpMethod.GET)
            .type(RouteGroup.TYPE_JSON)
            .accessControl(Python3RestEndpoints::checkReadPermission)  // ✅ AUTH (read-only)
            .mount();

        // GET /data/python3integration/api/v1/gateway-impact - Get Gateway impact assessment
        routes.newRoute("/api/v1/gateway-impact")
            .handler(Python3RestEndpoints::handleGetGatewayImpact)
            .method(HttpMethod.GET)
            .type(RouteGroup.TYPE_JSON)
            .accessControl(Python3RestEndpoints::checkReadPermission)  // ✅ AUTH (read-only)
            .mount();

        // GET /data/python3integration/api/v1/metrics/script-metrics - Get per-script metrics (NEW v1.16.0)
        routes.newRoute("/api/v1/metrics/script-metrics")
            .handler(Python3RestEndpoints::handleGetScriptMetrics)
            .method(HttpMethod.GET)
            .type(RouteGroup.TYPE_JSON)
            .accessControl(Python3RestEndpoints::checkReadPermission)
            .mount();

        // GET /data/python3integration/api/v1/metrics/historical - Get historical metrics (NEW v1.16.0)
        routes.newRoute("/api/v1/metrics/historical")
            .handler(Python3RestEndpoints::handleGetHistoricalMetrics)
            .method(HttpMethod.GET)
            .type(RouteGroup.TYPE_JSON)
            .accessControl(Python3RestEndpoints::checkReadPermission)
            .mount();

        // GET /data/python3integration/api/v1/metrics/alerts - Get active health alerts (NEW v1.16.0)
        routes.newRoute("/api/v1/metrics/alerts")
            .handler(Python3RestEndpoints::handleGetHealthAlerts)
            .method(HttpMethod.GET)
            .type(RouteGroup.TYPE_JSON)
            .accessControl(Python3RestEndpoints::checkReadPermission)
            .mount();

        // GET /data/python3integration/api/v1/monitoring/metrics - Get enhanced metrics (NEW v2.14.0 Phase 2 Week 3-4)
        routes.newRoute("/api/v1/monitoring/metrics")
            .handler(Python3RestEndpoints::handleGetEnhancedMetrics)
            .method(HttpMethod.GET)
            .type(RouteGroup.TYPE_JSON)
            .accessControl(Python3RestEndpoints::checkReadPermission)
            .mount();

        // GET /data/python3integration/api/v1/monitoring/circuit-breaker - Get circuit breaker status (NEW v2.14.0 Phase 2 Week 3-4)
        routes.newRoute("/api/v1/monitoring/circuit-breaker")
            .handler(Python3RestEndpoints::handleGetCircuitBreakerStatus)
            .method(HttpMethod.GET)
            .type(RouteGroup.TYPE_JSON)
            .accessControl(Python3RestEndpoints::checkReadPermission)
            .mount();

        // GET /data/python3integration/api/v1/monitoring/alerts - Get alert manager status (NEW v2.14.0 Phase 2 Week 3-4)
        routes.newRoute("/api/v1/monitoring/alerts")
            .handler(Python3RestEndpoints::handleGetAlertManagerStatus)
            .method(HttpMethod.GET)
            .type(RouteGroup.TYPE_JSON)
            .accessControl(Python3RestEndpoints::checkReadPermission)
            .mount();

        // GET /data/python3integration/api/v1/monitoring/prometheus - Get Prometheus metrics (NEW v2.15.0 Phase 3 Week 1-2)
        routes.newRoute("/api/v1/monitoring/prometheus")
            .handler(Python3RestEndpoints::handleGetPrometheusMetrics)
            .method(HttpMethod.GET)
            .type(RouteGroup.TYPE_JSON)  // Handler will override content-type to text/plain
            .accessControl(Python3RestEndpoints::checkReadPermission)
            .mount();

        // GET /data/python3integration/api/v1/logs - Gateway log viewer (v3.5.0)
        routes.newRoute("/api/v1/logs")
            .handler(Python3RestEndpoints::handleGetLogs)
            .method(HttpMethod.GET)
            .type(RouteGroup.TYPE_JSON)
            .accessControl(Python3RestEndpoints::checkReadPermission)
            .mount();

        // Script Management Endpoints

        // POST /data/python3integration/api/v1/scripts/save - Save a script
        routes.newRoute("/api/v1/scripts/save")
            .handler(Python3RestEndpoints::handleSaveScript)
            .method(HttpMethod.POST)
            .type(RouteGroup.TYPE_JSON)
            .accessControl(Python3RestEndpoints::checkManagePermission)  // ✅ AUTH + RATE LIMIT
            .mount();

        // GET /data/python3integration/api/v1/scripts/load/{name} - Load a script
        routes.newRoute("/api/v1/scripts/load/:name")
            .handler(Python3RestEndpoints::handleLoadScript)
            .method(HttpMethod.GET)
            .type(RouteGroup.TYPE_JSON)
            .accessControl(Python3RestEndpoints::checkReadPermission)  // ✅ AUTH (read-only)
            .mount();

        // GET /data/python3integration/api/v1/scripts/list - List all scripts
        routes.newRoute("/api/v1/scripts/list")
            .handler(Python3RestEndpoints::handleListScripts)
            .method(HttpMethod.GET)
            .type(RouteGroup.TYPE_JSON)
            .accessControl(Python3RestEndpoints::checkReadPermission)  // ✅ AUTH (read-only)
            .mount();

        // DELETE /data/python3integration/api/v1/scripts/delete/{name} - Delete a script
        routes.newRoute("/api/v1/scripts/delete/:name")
            .handler(Python3RestEndpoints::handleDeleteScript)
            .method(HttpMethod.DELETE)
            .type(RouteGroup.TYPE_JSON)
            .accessControl(Python3RestEndpoints::checkManagePermission)  // ✅ AUTH + RATE LIMIT
            .mount();

        // GET /data/python3integration/api/v1/scripts/available - Get available scripts (NEW v2.0.24)
        routes.newRoute("/api/v1/scripts/available")
            .handler(Python3RestEndpoints::handleGetAvailableScripts)
            .method(HttpMethod.GET)
            .type(RouteGroup.TYPE_JSON)
            .accessControl(Python3RestEndpoints::checkReadPermission)  // ✅ AUTH (read-only)
            .mount();

        // Package Management Endpoints (NEW v2.3.0)

        // GET /data/python3integration/api/v1/packages/catalog - Get available packages
        routes.newRoute("/api/v1/packages/catalog")
            .handler(Python3RestEndpoints::handleGetPackageCatalog)
            .method(HttpMethod.GET)
            .type(RouteGroup.TYPE_JSON)
            .accessControl(Python3RestEndpoints::checkReadPermission)  // ✅ AUTH (read-only)
            .mount();

        // GET /data/python3integration/api/v1/packages/status - Get package installation status
        routes.newRoute("/api/v1/packages/status")
            .handler(Python3RestEndpoints::handleGetPackageStatus)
            .method(HttpMethod.GET)
            .type(RouteGroup.TYPE_JSON)
            .accessControl(Python3RestEndpoints::checkReadPermission)  // ✅ AUTH (read-only)
            .mount();

        // POST /data/python3integration/api/v1/packages/install/:name - Install a package bundle
        routes.newRoute("/api/v1/packages/install/:name")
            .handler(Python3RestEndpoints::handleInstallPackage)
            .method(HttpMethod.POST)
            .type(RouteGroup.TYPE_JSON)
            .accessControl(Python3RestEndpoints::checkManagePermission)  // ✅ AUTH + RATE LIMIT
            .mount();

        // POST /data/python3integration/api/v1/packages/uninstall/:name - Uninstall a package bundle
        routes.newRoute("/api/v1/packages/uninstall/:name")
            .handler(Python3RestEndpoints::handleUninstallPackage)
            .method(HttpMethod.POST)
            .type(RouteGroup.TYPE_JSON)
            .accessControl(Python3RestEndpoints::checkManagePermission)  // ✅ AUTH + RATE LIMIT
            .mount();

        // POST /data/python3integration/api/v1/packages/verify - Verify installed packages
        routes.newRoute("/api/v1/packages/verify")
            .handler(Python3RestEndpoints::handleVerifyPackages)
            .method(HttpMethod.POST)
            .type(RouteGroup.TYPE_JSON)
            .accessControl(Python3RestEndpoints::checkReadPermission)  // ✅ AUTH (read-only, verification)
            .mount();

        // GET /data/python3integration/api/v1/packages/search-pypi - Search PyPI (v3.3.0, updated v3.5.0)
        routes.newRoute("/api/v1/packages/search-pypi")
            .handler(Python3RestEndpoints::handleSearchPyPI)
            .method(HttpMethod.GET)
            .type(RouteGroup.TYPE_JSON)
            .accessControl(Python3RestEndpoints::checkReadPermission)
            .mount();

        // GET /data/python3integration/api/v1/packages/pypi-info/{name} - PyPI package metadata (v3.5.0)
        routes.newRoute("/api/v1/packages/pypi-info/:name")
            .handler(Python3RestEndpoints::handleGetPyPIInfo)
            .method(HttpMethod.GET)
            .type(RouteGroup.TYPE_JSON)
            .accessControl(Python3RestEndpoints::checkReadPermission)
            .mount();

        // ===== Python Distribution Management (v3.1.0) =====

        // GET /data/python3integration/api/v1/distributions - List all Python distributions with install status
        routes.newRoute("/api/v1/distributions")
            .handler(Python3RestEndpoints::handleGetDistributions)
            .method(HttpMethod.GET)
            .type(RouteGroup.TYPE_JSON)
            .accessControl(Python3RestEndpoints::checkReadPermission)
            .mount();

        // POST /data/python3integration/api/v1/distributions/install - Install a Python version
        routes.newRoute("/api/v1/distributions/install")
            .handler(Python3RestEndpoints::handleInstallDistribution)
            .method(HttpMethod.POST)
            .type(RouteGroup.TYPE_JSON)
            .accessControl(Python3RestEndpoints::checkManagePermission)
            .mount();

        // POST /data/python3integration/api/v1/distributions/uninstall - Uninstall a Python version
        routes.newRoute("/api/v1/distributions/uninstall")
            .handler(Python3RestEndpoints::handleUninstallDistribution)
            .method(HttpMethod.POST)
            .type(RouteGroup.TYPE_JSON)
            .accessControl(Python3RestEndpoints::checkManagePermission)
            .mount();

        LOGGER.info("Python3 REST API routes mounted successfully at /api/v1/*");
    }

    /**
     * Handle POST /exec - Execute Python code
     *
     * Request body: {"code": "...", "variables": {...}}
     * Response: {"success": true/false, "result": ..., "error": "..."}
     *
     * v1.17.0: Enhanced with security headers and CSRF protection
     */
    private static JsonObject handleExec(RequestContext req, HttpServletResponse res) {
        LOGGER.debug("REST API: /exec called");

        try {
            // v2.15.9: CSRF protection for state-changing operation
            validateCSRFIfSession(req);

            // SECURITY HEADERS: Apply to all responses (v1.17.0)
            applySecurityHeaders(res);

            JsonObject requestBody = parseJsonBody(req);
            String code = requestBody.has("code") ? requestBody.get("code").getAsString() : "";
            Map<String, Object> variables = new HashMap<>();

            if (requestBody.has("variables") && requestBody.get("variables").isJsonObject()) {
                variables = jsonToMap(requestBody.getAsJsonObject("variables"));
            }

            // v3.1.0: Optional Python version selection
            String pythonVersion = requestBody.has("version") ?
                requestBody.get("version").getAsString() : null;

            // INPUT VALIDATION: Validate code before execution
            validateCode(code);

            // SECURITY: Determine security mode based on authentication (v2.6.0)
            SecurityMode securityMode = determineSecurityMode(req);
            LOGGER.debug("Security mode for /exec: {}, version: {}", securityMode,
                pythonVersion != null ? pythonVersion : "default");

            // AUDIT LOG: Log code execution attempt
            auditLog("PYTHON_EXEC", code);

            Object result;
            if (pythonVersion != null) {
                result = scriptModule.exec(code, variables, securityMode.getValue(), pythonVersion);
            } else {
                result = scriptModule.exec(code, variables, securityMode.getValue());
            }

            JsonObject response = new JsonObject();
            response.addProperty("success", true);
            response.addProperty("result", result != null ? result.toString() : null);

            LOGGER.debug("REST API: /exec completed successfully");
            return response;

        } catch (Exception e) {
            LOGGER.error("REST API: /exec failed", e);
            applySecurityHeaders(res);  // Apply headers even on error
            return createErrorResponse(e.getMessage());
        }
    }

    /**
     * Handle POST /shell-exec - DEPRECATED AND DISABLED in v2.9.0 (SECURITY FIX: HIGH-02)
     * <p>
     * This endpoint has been permanently disabled due to critical security vulnerability.
     * Arbitrary shell command execution with shell=True allowed command injection attacks.
     * <p>
     * For safe subprocess execution, use Python's subprocess module via the /exec endpoint:
     * <pre>
     * import subprocess
     * result = subprocess.run(['ls', '-la'], capture_output=True, text=True)
     * </pre>
     *
     * @deprecated Removed in v2.9.0 for security reasons
     * @param req Request context
     * @param res HTTP response
     * @return Error response indicating this endpoint is disabled
     */
    @Deprecated
    private static JsonObject handleShellExec(RequestContext req, HttpServletResponse res) {
        LOGGER.warn("REST API: /shell-exec called (DISABLED - security vulnerability fixed in v2.9.0)");

        // v2.15.9: CSRF protection for state-changing operation (even though endpoint is disabled)
        validateCSRFIfSession(req);

        applySecurityHeaders(res);

        // Audit log the attempt to use disabled endpoint
        auditLog("SHELL_EXEC_DISABLED", "Attempt to use disabled shell-exec endpoint");

        JsonObject response = new JsonObject();
        response.addProperty("success", false);
        response.addProperty("error", "This endpoint has been disabled in v2.9.0 due to security vulnerability (HIGH-02: Arbitrary shell command execution)");
        response.addProperty("deprecated", true);
        response.addProperty("removed_in_version", "2.9.0");
        response.addProperty("security_issue", "Command injection vulnerability with shell=True");
        response.addProperty("alternative", "Use Python's subprocess module via /exec endpoint for safe subprocess execution");

        return response;
    }

    /**
     * Handles POST /data/python3integration/api/v1/shell-interactive/create
     * Creates a new interactive shell session.
     *
     * Request: {}
     * Response: {"success": true, "sessionId": "uuid"}
     *
     * v2.5.8: Interactive shell session creation
     */
    private static JsonObject handleCreateShellSession(RequestContext req, HttpServletResponse res) {
        LOGGER.debug("REST API: /shell-interactive/create called");

        try {
            // v2.15.9: CSRF protection for state-changing operation
            validateCSRFIfSession(req);

            // Apply security headers
            applySecurityHeaders(res);

            // Audit log
            auditLog("SHELL_INTERACTIVE_CREATE", "Creating new Python shell session");

            // Parse optional pythonVersion from request body (v3.1.1)
            JsonObject requestBody = parseJsonBody(req);
            String pythonVersion = null;
            if (requestBody != null && requestBody.has("pythonVersion")) {
                pythonVersion = requestBody.get("pythonVersion").getAsString();
            }

            // Resolve Python executable path
            String pythonPath = resolvePythonPath(pythonVersion);

            LOGGER.info("REST API: Creating interactive Python shell session with: {}", pythonPath);

            // Create session
            String sessionId = Python3InteractiveShell.createSession(pythonPath);

            if (sessionId == null) {
                return createErrorResponse("Failed to create shell session");
            }

            JsonObject response = new JsonObject();
            response.addProperty("success", true);
            response.addProperty("sessionId", sessionId);
            response.addProperty("pythonPath", pythonPath);

            LOGGER.info("REST API: Created interactive Python shell session: {} (python: {})", sessionId, pythonPath);
            return response;

        } catch (Exception e) {
            LOGGER.error("REST API: /shell-interactive/create failed", e);
            applySecurityHeaders(res);
            return createErrorResponse("Failed to create shell session: " + e.getMessage());
        }
    }

    /**
     * Resolves the Python executable path from an optional version string.
     * Falls back through: distributionManager.getVersionExecutablePath → distributionManager.getPythonPath → "python3"
     *
     * @param pythonVersion optional version string, e.g. "3.11" (may be null or blank)
     * @return resolved path to the Python executable
     */
    private static String resolvePythonPath(String pythonVersion) {
        if (pythonVersion != null && !pythonVersion.trim().isEmpty() && distributionManager != null) {
            String path = distributionManager.getVersionExecutablePath(pythonVersion.trim());
            if (path != null) {
                return path;
            }
        }

        if (distributionManager != null) {
            try {
                return distributionManager.getPythonPath();
            } catch (Exception e) {
                LOGGER.warn("Could not resolve default Python path from distributionManager: {}", e.getMessage());
            }
        }

        return "python3";
    }

    /**
     * Handles POST /data/python3integration/api/v1/shell-interactive/exec
     * Executes a command in an existing interactive shell session.
     *
     * Request: {"sessionId": "uuid", "command": "ls -la"}
     * Response: {"success": true, "output": "..."}
     *
     * v2.5.8: Interactive shell command execution
     */
    private static JsonObject handleInteractiveShellExec(RequestContext req, HttpServletResponse res) {
        LOGGER.debug("REST API: /shell-interactive/exec called");

        try {
            // v2.15.9: CSRF protection for state-changing operation
            validateCSRFIfSession(req);

            // Apply security headers
            applySecurityHeaders(res);

            JsonObject requestBody = parseJsonBody(req);
            String sessionId = requestBody.has("sessionId") ? requestBody.get("sessionId").getAsString() : "";
            String command = requestBody.has("command") ? requestBody.get("command").getAsString() : "";

            if (sessionId == null || sessionId.trim().isEmpty()) {
                return createErrorResponse("Missing required parameter: sessionId");
            }

            if (command == null || command.trim().isEmpty()) {
                return createErrorResponse("Missing required parameter: command");
            }

            LOGGER.info("REST API: Executing interactive shell command (session: {}): {}", sessionId, command);

            // Audit log
            auditLog("SHELL_INTERACTIVE_EXEC", "Session: " + sessionId + ", Command: " + command);

            // Execute command in session
            String output = Python3InteractiveShell.executeCommand(sessionId, command);

            JsonObject response = new JsonObject();
            response.addProperty("success", true);
            response.addProperty("output", output);

            LOGGER.info("REST API: Interactive shell command completed (session: {})", sessionId);
            return response;

        } catch (Exception e) {
            LOGGER.error("REST API: /shell-interactive/exec failed", e);
            applySecurityHeaders(res);
            return createErrorResponse("Interactive shell execution failed: " + e.getMessage());
        }
    }

    /**
     * Handles POST /data/python3integration/api/v1/shell-interactive/close
     * Closes an interactive shell session.
     *
     * Request: {"sessionId": "uuid"}
     * Response: {"success": true}
     *
     * v2.5.8: Interactive shell session closure
     */
    private static JsonObject handleCloseShellSession(RequestContext req, HttpServletResponse res) {
        LOGGER.debug("REST API: /shell-interactive/close called");

        try {
            // v2.15.9: CSRF protection for state-changing operation
            validateCSRFIfSession(req);

            // Apply security headers
            applySecurityHeaders(res);

            JsonObject requestBody = parseJsonBody(req);
            String sessionId = requestBody.has("sessionId") ? requestBody.get("sessionId").getAsString() : "";

            if (sessionId == null || sessionId.trim().isEmpty()) {
                return createErrorResponse("Missing required parameter: sessionId");
            }

            LOGGER.info("REST API: Closing interactive shell session: {}", sessionId);

            // Audit log
            auditLog("SHELL_INTERACTIVE_CLOSE", "Session: " + sessionId);

            // Close session
            Python3InteractiveShell.closeSession(sessionId);

            JsonObject response = new JsonObject();
            response.addProperty("success", true);

            LOGGER.info("REST API: Interactive shell session closed: {}", sessionId);
            return response;

        } catch (Exception e) {
            LOGGER.error("REST API: /shell-interactive/close failed", e);
            applySecurityHeaders(res);
            return createErrorResponse("Failed to close shell session: " + e.getMessage());
        }
    }

    /**
     * Handle POST /eval - Evaluate Python expression
     *
     * Request body: {"expression": "...", "variables": {...}}
     * Response: {"success": true/false, "result": ..., "error": "..."}
     */
    private static JsonObject handleEval(RequestContext req, HttpServletResponse res) {
        LOGGER.debug("REST API: /eval called");

        try {
            // v2.15.9: CSRF protection for state-changing operation
            validateCSRFIfSession(req);

            JsonObject requestBody = parseJsonBody(req);
            String expression = requestBody.has("expression") ? requestBody.get("expression").getAsString() : "";
            Map<String, Object> variables = new HashMap<>();

            if (requestBody.has("variables") && requestBody.get("variables").isJsonObject()) {
                variables = jsonToMap(requestBody.getAsJsonObject("variables"));
            }

            // v3.1.0: Optional Python version selection
            String pythonVersion = requestBody.has("version") ?
                requestBody.get("version").getAsString() : null;

            // INPUT VALIDATION: Validate expression before evaluation
            validateCode(expression);

            // SECURITY: Determine security mode based on authentication (v2.6.0)
            SecurityMode securityMode = determineSecurityMode(req);
            LOGGER.debug("Security mode for /eval: {}, version: {}", securityMode,
                pythonVersion != null ? pythonVersion : "default");

            // AUDIT LOG: Log expression evaluation
            auditLog("PYTHON_EVAL", expression);

            Object result;
            if (pythonVersion != null) {
                result = scriptModule.eval(expression, variables, securityMode.getValue(), pythonVersion);
            } else {
                result = scriptModule.eval(expression, variables, securityMode.getValue());
            }

            JsonObject response = new JsonObject();
            response.addProperty("success", true);
            response.addProperty("result", result != null ? result.toString() : null);

            LOGGER.debug("REST API: /eval completed successfully");
            return response;

        } catch (Exception e) {
            LOGGER.error("REST API: /eval failed", e);
            return createErrorResponse(e.getMessage());
        }
    }

    /**
     * Handle POST /call-module - Call Python module function
     *
     * Request body: {"module": "...", "function": "...", "args": [...]}
     * Response: {"success": true/false, "result": ..., "error": "..."}
     */
    private static JsonObject handleCallModule(RequestContext req, HttpServletResponse res) {
        LOGGER.debug("REST API: /call-module called");

        try {
            // v2.15.9: CSRF protection for state-changing operation
            validateCSRFIfSession(req);

            JsonObject requestBody = parseJsonBody(req);
            String moduleName = requestBody.has("module") ? requestBody.get("module").getAsString() : "";
            String functionName = requestBody.has("function") ? requestBody.get("function").getAsString() : "";
            List<Object> args = new ArrayList<>();

            if (requestBody.has("args") && requestBody.get("args").isJsonArray()) {
                JsonArray jsonArgs = requestBody.getAsJsonArray("args");
                for (JsonElement element : jsonArgs) {
                    args.add(jsonElementToObject(element));
                }
            }

            // SECURITY: Determine security mode based on authentication (v2.6.0)
            SecurityMode securityMode = determineSecurityMode(req);
            LOGGER.debug("Security mode for /call-module: {}", securityMode);

            // AUDIT LOG: Log module call
            auditLog("PYTHON_CALL_MODULE", moduleName + "." + functionName + "(" + args + ")");

            Object result = scriptModule.callModule(moduleName, functionName, args, Collections.emptyMap(), securityMode.getValue());

            JsonObject response = new JsonObject();
            response.addProperty("success", true);
            response.addProperty("result", result != null ? result.toString() : null);

            LOGGER.debug("REST API: /call-module completed successfully");
            return response;

        } catch (Exception e) {
            LOGGER.error("REST API: /call-module failed", e);
            return createErrorResponse(e.getMessage());
        }
    }

    /**
     * Handle POST /call-script - Call saved Python script by path
     *
     * Request body: {"scriptPath": "...", "args": [...], "kwargs": {...}}
     * Response: {"success": true/false, "result": ..., "error": "..."}
     */
    private static JsonObject handleCallScript(RequestContext req, HttpServletResponse res) {
        LOGGER.debug("REST API: /call-script called");

        try {
            // v2.15.9: CSRF protection for state-changing operation
            validateCSRFIfSession(req);

            JsonObject requestBody = parseJsonBody(req);
            String scriptPath = requestBody.has("scriptPath") ? requestBody.get("scriptPath").getAsString() : "";

            if (scriptPath.isEmpty()) {
                return createErrorResponse("scriptPath is required");
            }

            List<Object> args = new ArrayList<>();
            if (requestBody.has("args") && requestBody.get("args").isJsonArray()) {
                JsonArray jsonArgs = requestBody.getAsJsonArray("args");
                for (JsonElement element : jsonArgs) {
                    args.add(jsonElementToObject(element));
                }
            }

            Map<String, Object> kwargs = new HashMap<>();
            if (requestBody.has("kwargs") && requestBody.get("kwargs").isJsonObject()) {
                kwargs = jsonToMap(requestBody.getAsJsonObject("kwargs"));
            }

            // AUDIT LOG: Log script execution
            auditLog("PYTHON_CALL_SCRIPT", "scriptPath=" + scriptPath);

            Object result = scriptModule.callScript(scriptPath, args, kwargs);

            JsonObject response = new JsonObject();
            response.addProperty("success", true);
            response.addProperty("result", result != null ? result.toString() : null);

            LOGGER.debug("REST API: /call-script completed successfully for script: {}", scriptPath);
            return response;

        } catch (Exception e) {
            LOGGER.error("REST API: /call-script failed", e);
            return createErrorResponse(e.getMessage());
        }
    }

    /**
     * Handle GET /version - Get Python version
     *
     * Response: {"version": "...", "pythonVersion": "...", "available": true/false}
     *
     * v1.17.1: Fix - ensure pythonVersion field is always present
     */
    private static JsonObject handleGetVersion(RequestContext req, HttpServletResponse res) {
        LOGGER.debug("REST API: /version called");

        try {
            Map<String, Object> versionInfo = scriptModule.getVersion();
            JsonObject response = mapToJson(versionInfo);

            // Ensure pythonVersion field exists for client compatibility (v1.17.1)
            if (response.has("python_version") && !response.has("pythonVersion")) {
                response.addProperty("pythonVersion", response.get("python_version").getAsString());
            }
            if (response.has("version") && !response.has("pythonVersion")) {
                response.addProperty("pythonVersion", response.get("version").getAsString());
            }

            LOGGER.debug("REST API: /version completed successfully");
            return response;

        } catch (Exception e) {
            LOGGER.error("REST API: /version failed", e);
            return createErrorResponse(e.getMessage());
        }
    }

    /**
     * Handle GET /pool-stats - Get process pool statistics
     *
     * Response: {"poolSize": ..., "available": ..., "inUse": ..., "healthy": ...}
     */
    private static JsonObject handleGetPoolStats(RequestContext req, HttpServletResponse res) {
        LOGGER.debug("REST API: /pool-stats called");

        try {
            Map<String, Object> poolStats = scriptModule.getPoolStats();
            JsonObject response = mapToJson(poolStats);

            // Ensure healthCheckStatus field is present for the frontend (v3.5.2)
            if (!response.has("healthCheckStatus")) {
                boolean available = scriptModule.isAvailable();
                response.addProperty("healthCheckStatus", available ? "Healthy" : "Down");
            }

            LOGGER.debug("REST API: /pool-stats completed successfully");
            return response;

        } catch (Exception e) {
            LOGGER.error("REST API: /pool-stats failed", e);
            return createErrorResponse(e.getMessage());
        }
    }

    /**
     * Handle POST /pool-size - Set process pool size
     *
     * Request body: {"size": ...}
     * Response: {"success": true/false, "poolSize": ..., "message": "..."}
     *
     * v1.17.2: New endpoint for dynamic pool size adjustment (1-20)
     */
    private static JsonObject handleSetPoolSize(RequestContext req, HttpServletResponse res) {
        LOGGER.debug("REST API: /pool-size called");

        try {
            // v2.15.9: CSRF protection for state-changing operation
            validateCSRFIfSession(req);

            JsonObject requestBody = parseJsonBody(req);

            if (!requestBody.has("size")) {
                res.setStatus(400);
                return createErrorResponse("Pool size parameter is required");
            }

            int newSize = requestBody.get("size").getAsInt();

            // Validate pool size range
            if (newSize < 1 || newSize > 20) {
                res.setStatus(400);
                return createErrorResponse("Pool size must be between 1 and 20");
            }

            // AUDIT LOG: Log pool size change
            auditLog("POOL_SIZE_CHANGE", "newSize=" + newSize);

            // Resize the pool via script module
            scriptModule.resizePool(newSize);

            JsonObject response = new JsonObject();
            response.addProperty("success", true);
            response.addProperty("poolSize", newSize);
            response.addProperty("message", "Pool size changed to " + newSize);

            LOGGER.info("REST API: Pool size changed to {}", newSize);
            return response;

        } catch (SecurityException e) {
            LOGGER.warn("REST API: /pool-size CSRF validation failed: {}", e.getMessage());
            res.setStatus(403);
            return createErrorResponse(e.getMessage());
        } catch (Exception e) {
            LOGGER.error("REST API: /pool-size failed", e);
            res.setStatus(500);
            return createErrorResponse(e.getMessage());
        }
    }

    /**
     * Handle GET /health - Health check
     *
     * Response: {"healthy": true/false, "available": true/false}
     */
    private static JsonObject handleHealthCheck(RequestContext req, HttpServletResponse res) {
        LOGGER.debug("REST API: /health called");

        try {
            boolean available = scriptModule.isAvailable();

            JsonObject response = new JsonObject();
            response.addProperty("healthy", available);
            response.addProperty("available", available);
            response.addProperty("status", available ? "HEALTHY" : "DOWN");
            response.addProperty("timestamp", System.currentTimeMillis());

            LOGGER.debug("REST API: /health completed successfully");
            return response;

        } catch (Exception e) {
            LOGGER.error("REST API: /health failed", e);
            return createErrorResponse(e.getMessage());
        }
    }

    /**
     * Handle GET /versions - Available Python versions (v3.1.0)
     *
     * Response: {"versions": ["3.10", "3.11", "3.12"], "default": "3.11", "details": {...}}
     */
    private static JsonObject handleGetVersions(RequestContext req, HttpServletResponse res) {
        LOGGER.debug("REST API: /versions called");

        try {
            JsonObject response = new JsonObject();

            if (poolManager != null) {
                java.util.List<String> versions = poolManager.getAvailableVersions();

                JsonArray versionArray = new JsonArray();
                for (String v : versions) {
                    versionArray.add(v);
                }
                response.add("versions", versionArray);
                response.addProperty("default", poolManager.getDefaultVersion());
                response.addProperty("count", versions.size());

                // Per-version details
                JsonObject details = new JsonObject();
                for (String v : versions) {
                    JsonObject versionDetail = new JsonObject();
                    String path = poolManager.getPythonPath(v);
                    versionDetail.addProperty("path", path != null ? path : "unknown");
                    versionDetail.addProperty("isDefault", v.equals(poolManager.getDefaultVersion()));
                    try {
                        Python3ProcessPool pool = poolManager.getPool(v);
                        Python3ProcessPool.PoolStats stats = pool.getStats();
                        versionDetail.addProperty("poolSize", stats.totalSize);
                        versionDetail.addProperty("available", stats.available);
                        versionDetail.addProperty("healthy", stats.healthy);
                    } catch (PoolManager.VersionNotAvailableException e) {
                        versionDetail.addProperty("error", e.getMessage());
                    }
                    details.add(v, versionDetail);
                }
                response.add("details", details);
            } else {
                // Fallback: single version mode
                JsonArray versionArray = new JsonArray();
                java.util.List<String> versions = scriptModule.getAvailableVersions();
                if (versions.isEmpty()) {
                    versionArray.add("default");
                } else {
                    for (String v : versions) {
                        versionArray.add(v);
                    }
                }
                response.add("versions", versionArray);
                response.addProperty("default", "default");
                response.addProperty("count", versionArray.size());
            }

            response.addProperty("success", true);
            response.addProperty("timestamp", System.currentTimeMillis());

            LOGGER.debug("REST API: /versions completed successfully");
            return response;

        } catch (Exception e) {
            LOGGER.error("REST API: /versions failed", e);
            return createErrorResponse(e.getMessage());
        }
    }

    /**
     * Handle GET /diagnostics - Performance diagnostics
     *
     * Response: comprehensive diagnostic information
     */
    private static JsonObject handleDiagnostics(RequestContext req, HttpServletResponse res) {
        LOGGER.debug("REST API: /diagnostics called");

        try {
            JsonObject response = new JsonObject();

            // Module availability
            boolean available = scriptModule.isAvailable();
            response.addProperty("available", available);

            // Process pool stats
            Map<String, Object> poolStats = scriptModule.getPoolStats();
            response.add("poolStats", mapToJson(poolStats));

            // Python version info
            Map<String, Object> versionInfo = scriptModule.getVersion();
            response.add("versionInfo", mapToJson(versionInfo));

            // Distribution info
            Map<String, Object> distributionInfo = scriptModule.getDistributionInfo();
            response.add("distributionInfo", mapToJson(distributionInfo));

            // Timestamp
            response.addProperty("timestamp", System.currentTimeMillis());

            LOGGER.debug("REST API: /diagnostics completed successfully");
            return response;

        } catch (Exception e) {
            LOGGER.error("REST API: /diagnostics failed", e);
            return createErrorResponse(e.getMessage());
        }
    }

    /**
     * Handle GET /example - Run example test
     *
     * Response: {"success": true/false, "result": "..."}
     */
    private static JsonObject handleExample(RequestContext req, HttpServletResponse res) {
        LOGGER.debug("REST API: /example called");

        try {

            // AUDIT LOG: Log example execution
            auditLog("PYTHON_EXAMPLE", "Example test execution");

            String result = scriptModule.example();

            JsonObject response = new JsonObject();
            response.addProperty("success", true);
            response.addProperty("result", result);

            LOGGER.debug("REST API: /example completed successfully");
            return response;

        } catch (Exception e) {
            LOGGER.error("REST API: /example failed", e);
            return createErrorResponse(e.getMessage());
        }
    }

    /**
     * Handle POST /check-syntax - Check Python code syntax
     *
     * Request body: {"code": "..."}
     * Response: {"success": true, "errors": [{line, column, message, severity}, ...]}
     */
    private static JsonObject handleCheckSyntax(RequestContext req, HttpServletResponse res) {
        LOGGER.debug("REST API: /check-syntax called");

        try {
            JsonObject requestBody = parseJsonBody(req);
            String code = requestBody.has("code") ? requestBody.get("code").getAsString() : "";

            if (code.isEmpty()) {
                JsonObject response = new JsonObject();
                response.addProperty("success", true);
                response.add("errors", new JsonArray());
                return response;
            }

            // INPUT VALIDATION: Validate code size
            validateCode(code);

            // AUDIT LOG: Log syntax check (execution context)
            auditLog("PYTHON_CHECK_SYNTAX", code);

            // Call Python syntax checker through script module
            Map<String, Object> result = scriptModule.checkSyntax(code);

            JsonObject response = new JsonObject();
            response.addProperty("success", true);

            // Convert errors list to JSON array
            JsonArray errorsArray = new JsonArray();
            if (result.containsKey("errors") && result.get("errors") instanceof List) {
                // Safe cast: checkSyntax() returns Map with typed List from Python
                @SuppressWarnings("unchecked")
                List<Map<String, Object>> errors = (List<Map<String, Object>>) result.get("errors");

                for (Map<String, Object> error : errors) {
                    JsonObject errorJson = mapToJson(error);
                    errorsArray.add(errorJson);
                }
            }
            response.add("errors", errorsArray);

            LOGGER.debug("REST API: /check-syntax completed successfully, found {} errors",
                        errorsArray.size());
            return response;

        } catch (Exception e) {
            LOGGER.error("REST API: /check-syntax failed", e);
            return createErrorResponse(e.getMessage());
        }
    }

    /**
     * Handle POST /completions - Get code completions at cursor position
     *
     * Request body: {"code": "...", "line": 1, "column": 0}
     * Response: {"success": true, "completions": [{text, type, description, signature}, ...]}
     */
    private static JsonObject handleGetCompletions(RequestContext req, HttpServletResponse res) {
        LOGGER.debug("REST API: /completions called");

        try {
            JsonObject requestBody = parseJsonBody(req);
            String code = requestBody.has("code") ? requestBody.get("code").getAsString() : "";
            int line = requestBody.has("line") ? requestBody.get("line").getAsInt() : 1;
            int column = requestBody.has("column") ? requestBody.get("column").getAsInt() : 0;

            if (code.isEmpty()) {
                JsonObject response = new JsonObject();
                response.addProperty("success", true);
                response.add("completions", new JsonArray());
                response.addProperty("count", 0);
                return response;
            }

            // Call Python completion engine through script module
            Map<String, Object> result = scriptModule.getCompletions(code, line, column);

            JsonObject response = new JsonObject();
            response.addProperty("success", true);

            // Convert completions list to JSON array
            JsonArray completionsArray = new JsonArray();
            if (result.containsKey("completions") && result.get("completions") instanceof List) {
                // Safe cast: getCompletions() returns Map with typed List from Jedi
                @SuppressWarnings("unchecked")
                List<Map<String, Object>> completions = (List<Map<String, Object>>) result.get("completions");

                for (Map<String, Object> completion : completions) {
                    JsonObject completionJson = mapToJson(completion);
                    completionsArray.add(completionJson);
                }
            }
            response.add("completions", completionsArray);
            response.addProperty("count", completionsArray.size());

            // Add message if Jedi not installed
            if (result.containsKey("message")) {
                response.addProperty("message", result.get("message").toString());
            }

            LOGGER.debug("REST API: /completions completed successfully, found {} completions",
                        completionsArray.size());
            return response;

        } catch (Exception e) {
            LOGGER.error("REST API: /completions failed", e);
            return createErrorResponse(e.getMessage());
        }
    }

    /**
     * Handle GET /metrics - Get performance metrics
     *
     * Response: {"total_executions": ..., "success_rate": ..., "health_score": ..., ...}
     */
    private static JsonObject handleGetMetrics(RequestContext req, HttpServletResponse res) {
        LOGGER.debug("REST API: /metrics called");

        try {
            Map<String, Object> metrics = metricsCollector.getMetrics();
            JsonObject response = mapToJson(metrics);

            LOGGER.debug("REST API: /metrics completed successfully");
            return response;

        } catch (Exception e) {
            LOGGER.error("REST API: /metrics failed", e);
            return createErrorResponse(e.getMessage());
        }
    }

    /**
     * Handle GET /gateway-impact - Get Gateway impact assessment
     *
     * Response: {"executions_per_minute": ..., "pool_utilization_percent": ..., "impact_level": "LOW|MEDIUM|HIGH", ...}
     */
    private static JsonObject handleGetGatewayImpact(RequestContext req, HttpServletResponse res) {
        LOGGER.debug("REST API: /gateway-impact called");

        try {
            Map<String, Object> impact = metricsCollector.getGatewayImpact();
            JsonObject response = mapToJson(impact);

            LOGGER.debug("REST API: /gateway-impact completed successfully");
            return response;

        } catch (Exception e) {
            LOGGER.error("REST API: /gateway-impact failed", e);
            return createErrorResponse(e.getMessage());
        }
    }

    /**
     * Handle GET /metrics/script-metrics - Get per-script performance metrics (NEW v1.16.0)
     *
     * Response: [{"script_identifier": "...", "total_executions": ..., "success_rate": ..., ...}, ...]
     */
    private static JsonObject handleGetScriptMetrics(RequestContext req, HttpServletResponse res) {
        LOGGER.debug("REST API: /metrics/script-metrics called");

        try {
            List<Map<String, Object>> scriptMetrics = metricsCollector.getScriptMetrics();

            JsonObject response = new JsonObject();
            response.addProperty("success", true);

            JsonArray metricsArray = new JsonArray();
            for (Map<String, Object> metrics : scriptMetrics) {
                metricsArray.add(mapToJson(metrics));
            }
            response.add("script_metrics", metricsArray);
            response.addProperty("count", metricsArray.size());

            LOGGER.debug("REST API: /metrics/script-metrics completed successfully, {} scripts",
                    metricsArray.size());
            return response;

        } catch (Exception e) {
            LOGGER.error("REST API: /metrics/script-metrics failed", e);
            return createErrorResponse(e.getMessage());
        }
    }

    /**
     * Handle GET /metrics/historical - Get historical metric snapshots (NEW v1.16.0)
     *
     * Response: [{"timestamp": ..., "total_executions": ..., "pool_utilization": ..., ...}, ...]
     */
    private static JsonObject handleGetHistoricalMetrics(RequestContext req, HttpServletResponse res) {
        LOGGER.debug("REST API: /metrics/historical called");

        try {
            List<Map<String, Object>> historicalMetrics = metricsCollector.getHistoricalMetrics();

            JsonObject response = new JsonObject();
            response.addProperty("success", true);

            JsonArray historyArray = new JsonArray();
            for (Map<String, Object> snapshot : historicalMetrics) {
                historyArray.add(mapToJson(snapshot));
            }
            response.add("historical_metrics", historyArray);
            response.addProperty("count", historyArray.size());

            LOGGER.debug("REST API: /metrics/historical completed successfully, {} snapshots",
                    historyArray.size());
            return response;

        } catch (Exception e) {
            LOGGER.error("REST API: /metrics/historical failed", e);
            return createErrorResponse(e.getMessage());
        }
    }

    /**
     * Handle GET /metrics/alerts - Get active health alerts (NEW v1.16.0)
     *
     * Response: [{"timestamp": ..., "alert_id": "...", "message": "...", "severity": "WARNING|CRITICAL"}, ...]
     */
    private static JsonObject handleGetHealthAlerts(RequestContext req, HttpServletResponse res) {
        LOGGER.debug("REST API: /metrics/alerts called");

        try {
            List<Map<String, Object>> healthAlerts = metricsCollector.getHealthAlerts();

            JsonObject response = new JsonObject();
            response.addProperty("success", true);

            JsonArray alertsArray = new JsonArray();
            for (Map<String, Object> alert : healthAlerts) {
                alertsArray.add(mapToJson(alert));
            }
            response.add("alerts", alertsArray);
            response.addProperty("count", alertsArray.size());

            LOGGER.debug("REST API: /metrics/alerts completed successfully, {} active alerts",
                    alertsArray.size());
            return response;

        } catch (Exception e) {
            LOGGER.error("REST API: /metrics/alerts failed", e);
            return createErrorResponse(e.getMessage());
        }
    }

    // Script Management Handlers

    /**
     * Handle POST /scripts/save - Save a Python script
     *
     * Request body: {"name": "...", "code": "...", "description": "...", "author": "...", "folderPath": "...", "version": "..."}
     * Response: {"success": true/false, "script": {...}}
     */
    private static JsonObject handleSaveScript(RequestContext req, HttpServletResponse res) {
        LOGGER.debug("REST API: /scripts/save called");

        try {
            // v2.15.9: CSRF protection for state-changing operation
            validateCSRFIfSession(req);

            if (scriptRepository == null) {
                return createErrorResponse("Script repository not initialized");
            }

            JsonObject requestBody = parseJsonBody(req);
            String name = requestBody.has("name") ? requestBody.get("name").getAsString() : null;
            String code = requestBody.has("code") ? requestBody.get("code").getAsString() : "";
            String description = requestBody.has("description") ? requestBody.get("description").getAsString() : null;
            String author = requestBody.has("author") ? requestBody.get("author").getAsString() : "Unknown";
            String folderPath = requestBody.has("folderPath") ? requestBody.get("folderPath").getAsString() : "";
            String version = requestBody.has("version") ? requestBody.get("version").getAsString() : "1.0";

            if (name == null || name.trim().isEmpty()) {
                return createErrorResponse("Script name is required");
            }

            // INPUT VALIDATION: Validate all inputs
            validateScriptName(name);
            validateCode(code);
            validateFolderPath(folderPath);

            // AUDIT LOG: Log script save
            auditLog("SCRIPT_SAVE", "name=" + name + ", folder=" + folderPath + ", code_hash=" + hashCode(code));

            Python3ScriptRepository.SavedScript script = scriptRepository.saveScript(
                name, code, description, author, folderPath, version
            );

            JsonObject response = new JsonObject();
            response.addProperty("success", true);

            JsonObject scriptJson = new JsonObject();
            scriptJson.addProperty("id", script.getId());
            scriptJson.addProperty("name", script.getName());
            scriptJson.addProperty("description", script.getDescription());
            scriptJson.addProperty("author", script.getAuthor());
            scriptJson.addProperty("createdDate", script.getCreatedDate());
            scriptJson.addProperty("lastModified", script.getLastModified());
            scriptJson.addProperty("folderPath", script.getFolderPath());
            scriptJson.addProperty("version", script.getVersion());
            response.add("script", scriptJson);

            LOGGER.info("REST API: Script saved: {} in folder: {}", name, folderPath);
            return response;

        } catch (Exception e) {
            LOGGER.error("REST API: /scripts/save failed", e);
            return createErrorResponse(e.getMessage());
        }
    }

    /**
     * Handle GET /scripts/load/:name - Load a saved script
     *
     * Response: {"success": true/false, "script": {...}}
     */
    private static JsonObject handleLoadScript(RequestContext req, HttpServletResponse res) {
        LOGGER.debug("REST API: /scripts/load called");

        try {
            if (scriptRepository == null) {
                return createErrorResponse("Script repository not initialized");
            }

            // Extract name from URL path
            String requestPath = req.getRequest().getRequestURI();
            String name = requestPath.substring(requestPath.lastIndexOf('/') + 1);

            if (name == null || name.trim().isEmpty()) {
                return createErrorResponse("Script name is required");
            }

            Python3ScriptRepository.SavedScript script = scriptRepository.loadScript(name);

            if (script == null) {
                return createErrorResponse("Script not found: " + name);
            }

            JsonObject response = new JsonObject();
            response.addProperty("success", true);

            JsonObject scriptJson = new JsonObject();
            scriptJson.addProperty("id", script.getId());
            scriptJson.addProperty("name", script.getName());
            scriptJson.addProperty("code", script.getCode());
            scriptJson.addProperty("description", script.getDescription());
            scriptJson.addProperty("author", script.getAuthor());
            scriptJson.addProperty("createdDate", script.getCreatedDate());
            scriptJson.addProperty("lastModified", script.getLastModified());
            scriptJson.addProperty("folderPath", script.getFolderPath());
            scriptJson.addProperty("version", script.getVersion());
            response.add("script", scriptJson);

            LOGGER.debug("REST API: Script loaded: {}", name);
            return response;

        } catch (Exception e) {
            LOGGER.error("REST API: /scripts/load failed", e);
            return createErrorResponse(e.getMessage());
        }
    }

    /**
     * Handle GET /scripts/list - List all saved scripts
     *
     * Response: {"success": true, "scripts": [...]}
     */
    private static JsonObject handleListScripts(RequestContext req, HttpServletResponse res) {
        LOGGER.debug("REST API: /scripts/list called");

        try {
            if (scriptRepository == null) {
                return createErrorResponse("Script repository not initialized");
            }

            List<Python3ScriptRepository.ScriptMetadata> scripts = scriptRepository.listScripts();

            JsonObject response = new JsonObject();
            response.addProperty("success", true);

            JsonArray scriptsArray = new JsonArray();
            for (Python3ScriptRepository.ScriptMetadata script : scripts) {
                JsonObject scriptJson = new JsonObject();
                scriptJson.addProperty("id", script.getId());
                scriptJson.addProperty("name", script.getName());
                scriptJson.addProperty("description", script.getDescription());
                scriptJson.addProperty("author", script.getAuthor());
                scriptJson.addProperty("createdDate", script.getCreatedDate());
                scriptJson.addProperty("lastModified", script.getLastModified());
                scriptJson.addProperty("folderPath", script.getFolderPath());
                scriptJson.addProperty("version", script.getVersion());
                scriptsArray.add(scriptJson);
            }
            response.add("scripts", scriptsArray);

            LOGGER.debug("REST API: Listed {} scripts", scripts.size());
            return response;

        } catch (Exception e) {
            LOGGER.error("REST API: /scripts/list failed", e);
            return createErrorResponse(e.getMessage());
        }
    }

    /**
     * Handle DELETE /scripts/delete/:name - Delete a saved script
     *
     * Response: {"success": true/false}
     */
    private static JsonObject handleDeleteScript(RequestContext req, HttpServletResponse res) {
        LOGGER.debug("REST API: /scripts/delete called");

        try {
            // v2.15.9: CSRF protection for state-changing operation
            validateCSRFIfSession(req);

            if (scriptRepository == null) {
                return createErrorResponse("Script repository not initialized");
            }

            // Extract name from URL path
            String requestPath = req.getRequest().getRequestURI();
            String name = requestPath.substring(requestPath.lastIndexOf('/') + 1);

            if (name == null || name.trim().isEmpty()) {
                return createErrorResponse("Script name is required");
            }

            // AUDIT LOG: Log script deletion
            auditLog("SCRIPT_DELETE", "name=" + name);

            boolean deleted = scriptRepository.deleteScript(name);

            JsonObject response = new JsonObject();
            response.addProperty("success", deleted);

            if (!deleted) {
                response.addProperty("message", "Script not found: " + name);
            } else {
                response.addProperty("message", "Script deleted successfully");
            }

            LOGGER.info("REST API: Script deletion: {} - {}", name, deleted ? "success" : "not found");
            return response;

        } catch (Exception e) {
            LOGGER.error("REST API: /scripts/delete failed", e);
            return createErrorResponse(e.getMessage());
        }
    }

    /**
     * Handle GET /scripts/available - Get list of available scripts with metadata
     *
     * Response: {"success": true, "scripts": [{name, description, path, author, version}, ...]}
     *
     * v2.0.24: New endpoint for script autocomplete and discovery
     */
    private static JsonObject handleGetAvailableScripts(RequestContext req, HttpServletResponse res) {
        LOGGER.debug("REST API: /scripts/available called");

        try {
            if (scriptModule == null) {
                return createErrorResponse("Script module not initialized");
            }

            List<Map<String, Object>> scripts = scriptModule.getAvailableScripts();

            JsonObject response = new JsonObject();
            response.addProperty("success", true);

            JsonArray scriptsArray = new JsonArray();
            for (Map<String, Object> script : scripts) {
                scriptsArray.add(mapToJson(script));
            }
            response.add("scripts", scriptsArray);
            response.addProperty("count", scriptsArray.size());

            LOGGER.debug("REST API: /scripts/available completed successfully, {} scripts", scriptsArray.size());
            return response;

        } catch (Exception e) {
            LOGGER.error("REST API: /scripts/available failed", e);
            return createErrorResponse(e.getMessage());
        }
    }

    // Package Management Handlers (v2.3.0)

    /**
     * Handle GET /packages/catalog - Get available package bundles
     *
     * Response: {"success": true, "packages": {"jedi": {...}, "web": {...}, "datascience": {...}}}
     *
     * v2.3.0: New endpoint for package management
     */
    private static JsonObject handleGetPackageCatalog(RequestContext req, HttpServletResponse res) {
        LOGGER.debug("REST API: /packages/catalog called");

        try {
            if (packageManager == null) {
                return createErrorResponse("Package manager not initialized");
            }

            Map<String, Python3PackageManager.PackageInfo> catalog = packageManager.getPackageCatalog();

            JsonObject response = new JsonObject();
            response.addProperty("success", true);

            JsonObject packagesJson = new JsonObject();
            for (Map.Entry<String, Python3PackageManager.PackageInfo> entry : catalog.entrySet()) {
                String packageName = entry.getKey();
                Python3PackageManager.PackageInfo info = entry.getValue();

                JsonObject packageJson = new JsonObject();
                packageJson.addProperty("version", info.version);
                packageJson.addProperty("description", info.description);
                packageJson.addProperty("sizeMb", info.sizeMb);
                packageJson.addProperty("importName", info.importName);

                JsonArray wheelsArray = new JsonArray();
                for (String wheel : info.wheels) {
                    wheelsArray.add(wheel);
                }
                packageJson.add("wheels", wheelsArray);

                JsonArray pipPackagesArray = new JsonArray();
                for (String pipPkg : info.pipPackages) {
                    pipPackagesArray.add(pipPkg);
                }
                packageJson.add("pipPackages", pipPackagesArray);

                JsonArray requiredForArray = new JsonArray();
                for (String usage : info.requiredFor) {
                    requiredForArray.add(usage);
                }
                packageJson.add("requiredFor", requiredForArray);

                packagesJson.add(packageName, packageJson);
            }
            response.add("packages", packagesJson);
            response.addProperty("count", catalog.size());

            LOGGER.debug("REST API: /packages/catalog completed successfully, {} packages", catalog.size());
            return response;

        } catch (Exception e) {
            LOGGER.error("REST API: /packages/catalog failed", e);
            return createErrorResponse(e.getMessage());
        }
    }

    /**
     * Handle GET /packages/status - Get package installation status
     *
     * Response: {"success": true, "status": {...}, "installed": ["jedi", ...], "available": ["jedi", "web", ...]}
     *
     * v2.3.0: New endpoint for package management
     */
    private static JsonObject handleGetPackageStatus(RequestContext req, HttpServletResponse res) {
        LOGGER.debug("REST API: /packages/status called");

        try {
            if (packageManager == null) {
                return createErrorResponse("Package manager not initialized");
            }

            Map<String, Object> status = packageManager.getStatus();
            Set<String> installed = packageManager.getInstalledPackages();
            Map<String, Python3PackageManager.PackageInfo> catalog = packageManager.getPackageCatalog();

            JsonObject response = new JsonObject();
            response.addProperty("success", true);

            // Add status information
            response.add("status", mapToJson(status));

            // Add installed packages
            JsonArray installedArray = new JsonArray();
            for (String packageName : installed) {
                installedArray.add(packageName);
            }
            response.add("installed", installedArray);

            // Add available packages
            JsonArray availableArray = new JsonArray();
            for (String packageName : catalog.keySet()) {
                availableArray.add(packageName);
            }
            response.add("available", availableArray);

            LOGGER.debug("REST API: /packages/status completed successfully");
            return response;

        } catch (Exception e) {
            LOGGER.error("REST API: /packages/status failed", e);
            return createErrorResponse(e.getMessage());
        }
    }

    /**
     * Handle POST /packages/install/:name - Install a package bundle
     *
     * Response: {"success": true/false, "message": "...", "installedWheels": [...]}
     *
     * v2.3.0: New endpoint for package management
     */
    private static JsonObject handleInstallPackage(RequestContext req, HttpServletResponse res) {
        LOGGER.debug("REST API: /packages/install called");

        try {
            // v2.15.9: CSRF protection for state-changing operation
            validateCSRFIfSession(req);

            if (packageManager == null) {
                return createErrorResponse("Package manager not initialized");
            }

            // Extract package name from URL path
            String requestPath = req.getRequest().getRequestURI();
            String packageName = requestPath.substring(requestPath.lastIndexOf('/') + 1);

            if (packageName == null || packageName.trim().isEmpty()) {
                return createErrorResponse("Package name is required");
            }

            // AUDIT LOG: Log package installation
            auditLog("PACKAGE_INSTALL", "package=" + packageName);

            Python3PackageManager.InstallResult result = packageManager.installPackage(packageName);

            JsonObject response = new JsonObject();
            response.addProperty("success", result.success);
            response.addProperty("message", result.message);

            JsonArray wheelsArray = new JsonArray();
            for (String wheel : result.installedWheels) {
                wheelsArray.add(wheel);
            }
            response.add("installedWheels", wheelsArray);

            LOGGER.info("REST API: Package installation: {} - {}", packageName, result.success ? "success" : "failed");
            return response;

        } catch (Exception e) {
            LOGGER.error("REST API: /packages/install failed", e);
            return createErrorResponse(e.getMessage());
        }
    }

    /**
     * Handle POST /packages/uninstall/:name - Uninstall a package bundle
     *
     * Response: {"success": true/false, "message": "..."}
     *
     * v2.3.0: New endpoint for package management
     */
    private static JsonObject handleUninstallPackage(RequestContext req, HttpServletResponse res) {
        LOGGER.debug("REST API: /packages/uninstall called");

        try {
            // v2.15.9: CSRF protection for state-changing operation
            validateCSRFIfSession(req);

            if (packageManager == null) {
                return createErrorResponse("Package manager not initialized");
            }

            // Extract package name from URL path
            String requestPath = req.getRequest().getRequestURI();
            String packageName = requestPath.substring(requestPath.lastIndexOf('/') + 1);

            if (packageName == null || packageName.trim().isEmpty()) {
                return createErrorResponse("Package name is required");
            }

            // AUDIT LOG: Log package uninstallation
            auditLog("PACKAGE_UNINSTALL", "package=" + packageName);

            boolean success = packageManager.uninstallPackage(packageName);

            JsonObject response = new JsonObject();
            response.addProperty("success", success);
            response.addProperty("message", success ? "Package uninstalled successfully" : "Failed to uninstall package");

            LOGGER.info("REST API: Package uninstallation: {} - {}", packageName, success ? "success" : "failed");
            return response;

        } catch (Exception e) {
            LOGGER.error("REST API: /packages/uninstall failed", e);
            return createErrorResponse(e.getMessage());
        }
    }

    /**
     * Handle POST /packages/verify - Verify installed packages
     *
     * Response: {"success": true, "verification": {"jedi": true, "web": false, ...}}
     *
     * v2.3.0: New endpoint for package management
     */
    private static JsonObject handleVerifyPackages(RequestContext req, HttpServletResponse res) {
        LOGGER.debug("REST API: /packages/verify called");

        try {
            if (packageManager == null) {
                return createErrorResponse("Package manager not initialized");
            }

            Map<String, Boolean> verification = packageManager.verifyPackages();

            JsonObject response = new JsonObject();
            response.addProperty("success", true);

            JsonObject verificationJson = new JsonObject();
            for (Map.Entry<String, Boolean> entry : verification.entrySet()) {
                verificationJson.addProperty(entry.getKey(), entry.getValue());
            }
            response.add("verification", verificationJson);

            LOGGER.debug("REST API: /packages/verify completed successfully, verified {} packages", verification.size());
            return response;

        } catch (Exception e) {
            LOGGER.error("REST API: /packages/verify failed", e);
            return createErrorResponse(e.getMessage());
        }
    }

    /**
     * Handle GET /packages/search-pypi - Search PyPI for packages (v3.5.0)
     *
     * Strategy: Try exact match via PyPI JSON API first, then fall back to HTML search
     * with an updated regex pattern.
     *
     * Query parameter: q (search query)
     * Response: {"success": true, "query": "...", "count": N, "results": [{name, version, summary}, ...]}
     */
    private static JsonObject handleSearchPyPI(RequestContext req, HttpServletResponse res) {
        String query = req.getRequest().getParameter("q");
        LOGGER.debug("REST API: /packages/search-pypi called with q={}", query);

        if (query == null || query.trim().isEmpty()) {
            JsonObject response = new JsonObject();
            response.addProperty("success", true);
            response.addProperty("query", "");
            response.addProperty("count", 0);
            response.add("results", new JsonArray());
            return response;
        }

        query = query.trim();
        JsonArray results = new JsonArray();

        HttpClient client = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .followRedirects(HttpClient.Redirect.NORMAL)
            .build();

        try {
            // Step 1: Try exact match via PyPI JSON API
            JsonObject exactMatch = fetchPyPIPackageInfo(client, query);
            if (exactMatch != null) {
                results.add(exactMatch);
            }

            // Step 2: HTML search fallback for broader results
            String encoded = URLEncoder.encode(query, StandardCharsets.UTF_8);
            URI uri = URI.create("https://pypi.org/search/?q=" + encoded + "&page=1");

            HttpRequest request = HttpRequest.newBuilder()
                .uri(uri)
                .timeout(Duration.ofSeconds(15))
                .header("Accept", "text/html")
                .header("User-Agent", "IgnitionPython3Module/3.5.0")
                .GET()
                .build();

            HttpResponse<String> httpResponse = client.send(request, HttpResponse.BodyHandlers.ofString());
            String html = httpResponse.body();

            // v3.5.0: Updated regex patterns to match current PyPI HTML structure
            // Try multiple patterns for resilience against HTML changes
            Pattern[] patterns = {
                // Current PyPI structure (2024+)
                Pattern.compile(
                    "<a[^>]*class=\"[^\"]*package-snippet[^\"]*\"[^>]*>.*?" +
                    "<span[^>]*class=\"[^\"]*package-snippet__name[^\"]*\"[^>]*>\\s*([^<]+?)\\s*</span>.*?" +
                    "<span[^>]*class=\"[^\"]*package-snippet__version[^\"]*\"[^>]*>\\s*([^<]+?)\\s*</span>.*?" +
                    "<p[^>]*class=\"[^\"]*package-snippet__description[^\"]*\"[^>]*>\\s*([^<]*?)\\s*</p>",
                    Pattern.DOTALL
                ),
                // Alternative: h3/span based pattern
                Pattern.compile(
                    "<h3[^>]*class=\"[^\"]*package-snippet__title[^\"]*\"[^>]*>.*?" +
                    "<span[^>]*class=\"[^\"]*package-snippet__name[^\"]*\"[^>]*>\\s*([^<]+?)\\s*</span>.*?" +
                    "<span[^>]*class=\"[^\"]*package-snippet__version[^\"]*\"[^>]*>\\s*([^<]+?)\\s*</span>.*?" +
                    "<p[^>]*class=\"[^\"]*package-snippet__description[^\"]*\"[^>]*>\\s*([^<]*?)\\s*</p>",
                    Pattern.DOTALL
                ),
            };

            // Track names already added (from exact match)
            java.util.Set<String> addedNames = new java.util.HashSet<>();
            if (exactMatch != null) {
                addedNames.add(exactMatch.get("name").getAsString().toLowerCase());
            }

            int maxResults = 30;
            for (Pattern pattern : patterns) {
                Matcher matcher = pattern.matcher(html);
                while (matcher.find() && results.size() < maxResults) {
                    String name = matcher.group(1).trim();
                    String version = matcher.group(2).trim();
                    String summary = matcher.group(3).trim();

                    if (!addedNames.contains(name.toLowerCase())) {
                        JsonObject pkg = new JsonObject();
                        pkg.addProperty("name", name);
                        pkg.addProperty("version", version);
                        pkg.addProperty("summary", summary);
                        results.add(pkg);
                        addedNames.add(name.toLowerCase());
                    }
                }
                if (results.size() > 1) break;  // Found results with this pattern
            }

            JsonObject response = new JsonObject();
            response.addProperty("success", true);
            response.addProperty("query", query);
            response.addProperty("count", results.size());
            response.add("results", results);

            LOGGER.debug("REST API: /packages/search-pypi found {} results for '{}'", results.size(), query);
            return response;

        } catch (Exception e) {
            LOGGER.error("REST API: /packages/search-pypi failed for query '{}'", query, e);
            // Return whatever we have (might have exact match even if HTML failed)
            JsonObject response = new JsonObject();
            response.addProperty("success", true);
            response.addProperty("query", query);
            response.addProperty("count", results.size());
            response.add("results", results);
            if (results.size() == 0) {
                response.addProperty("warning", "Search failed: " + e.getMessage());
            }
            return response;
        }
    }

    /**
     * Fetch package info from PyPI JSON API.
     * Returns a result object with name, version, summary, or null if not found.
     */
    private static JsonObject fetchPyPIPackageInfo(HttpClient client, String packageName) {
        try {
            String encoded = URLEncoder.encode(packageName, StandardCharsets.UTF_8);
            URI uri = URI.create("https://pypi.org/pypi/" + encoded + "/json");

            HttpRequest request = HttpRequest.newBuilder()
                .uri(uri)
                .timeout(Duration.ofSeconds(10))
                .header("Accept", "application/json")
                .header("User-Agent", "IgnitionPython3Module/3.5.0")
                .GET()
                .build();

            HttpResponse<String> httpResponse = client.send(request, HttpResponse.BodyHandlers.ofString());
            if (httpResponse.statusCode() != 200) {
                return null;
            }

            JsonObject pypiData = JsonParser.parseString(httpResponse.body()).getAsJsonObject();
            JsonObject info = pypiData.getAsJsonObject("info");
            if (info == null) return null;

            JsonObject pkg = new JsonObject();
            pkg.addProperty("name", info.has("name") ? info.get("name").getAsString() : packageName);
            pkg.addProperty("version", info.has("version") ? info.get("version").getAsString() : "");
            pkg.addProperty("summary", info.has("summary") && !info.get("summary").isJsonNull()
                ? info.get("summary").getAsString() : "");
            pkg.addProperty("author", info.has("author") && !info.get("author").isJsonNull()
                ? info.get("author").getAsString() : "");
            pkg.addProperty("license", info.has("license") && !info.get("license").isJsonNull()
                ? info.get("license").getAsString() : "");
            pkg.addProperty("homepage", info.has("home_page") && !info.get("home_page").isJsonNull()
                ? info.get("home_page").getAsString() : "");

            // Collect available versions from releases
            if (pypiData.has("releases")) {
                JsonArray versions = new JsonArray();
                for (String ver : pypiData.getAsJsonObject("releases").keySet()) {
                    versions.add(ver);
                }
                pkg.add("versions", versions);
            }

            return pkg;
        } catch (Exception e) {
            LOGGER.debug("PyPI JSON API lookup failed for '{}': {}", packageName, e.getMessage());
            return null;
        }
    }

    /**
     * Handle GET /packages/pypi-info/{name} - Get full PyPI package metadata (v3.5.0)
     *
     * Returns detailed package info including all available versions.
     */
    private static JsonObject handleGetPyPIInfo(RequestContext req, HttpServletResponse res) {
        // Extract package name from path: /api/v1/packages/pypi-info/{name}
        String path = req.getRequest().getRequestURI();
        String name = path.substring(path.lastIndexOf('/') + 1);
        LOGGER.debug("REST API: /packages/pypi-info called for '{}'", name);

        if (name.isEmpty()) {
            return createErrorResponse("Package name is required");
        }

        HttpClient client = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .followRedirects(HttpClient.Redirect.NORMAL)
            .build();

        JsonObject pkg = fetchPyPIPackageInfo(client, name);
        if (pkg == null) {
            JsonObject response = new JsonObject();
            response.addProperty("success", false);
            response.addProperty("error", "Package '" + name + "' not found on PyPI");
            return response;
        }

        JsonObject response = new JsonObject();
        response.addProperty("success", true);
        response.add("package", pkg);
        return response;
    }

    /**
     * Handle GET /logs - Read gateway logs from wrapper.log (v3.5.0)
     *
     * Query parameters:
     *   lines  - max entries to return (default 100)
     *   level  - minimum level filter: ALL, DEBUG, INFO, WARN, ERROR (default ALL)
     *   filter - text search (case-insensitive)
     *   after  - line offset for pagination (default 0)
     */
    private static JsonObject handleGetLogs(RequestContext req, HttpServletResponse res) {
        LOGGER.debug("REST API: /logs called");

        try {
            String linesParam = req.getRequest().getParameter("lines");
            String levelParam = req.getRequest().getParameter("level");
            String filterParam = req.getRequest().getParameter("filter");
            String afterParam = req.getRequest().getParameter("after");

            int lines = 100;
            if (linesParam != null) {
                try { lines = Math.min(500, Math.max(1, Integer.parseInt(linesParam))); }
                catch (NumberFormatException ignored) {}
            }

            long after = 0;
            if (afterParam != null) {
                try { after = Math.max(0, Long.parseLong(afterParam)); }
                catch (NumberFormatException ignored) {}
            }

            return Python3LogsHandler.readLogs(logsDir, lines, levelParam, filterParam, after);

        } catch (Exception e) {
            LOGGER.error("REST API: /logs failed", e);
            return createErrorResponse("Failed to read logs: " + e.getMessage());
        }
    }

    // Authentication Handlers

    /**
     * Handle POST /auth/session - Create a new session token for Designer IDE
     * <p>
     * This endpoint allows the Designer IDE to obtain a secure session token
     * without relying on the spoofable User-Agent header.
     * <p>
     * Request body: {"client_id": "ignition-designer-{version}"}
     * Response: {"success": true, "token": "{signed-token}", "expires_in": 28800, "security_mode": "DESIGNER_ADMIN"}
     *
     * @since v2.9.0 - Security fix for CRITICAL-01 (User-Agent authentication bypass)
     */
    private static JsonObject handleCreateSession(RequestContext req, HttpServletResponse res) {
        LOGGER.debug("REST API: /auth/session called");

        try {
            JsonObject requestBody = parseJsonBody(req);

            // Validate client_id (accept Designer and Gateway Web UI clients)
            String clientId = requestBody.has("client_id") ? requestBody.get("client_id").getAsString() : null;
            if (clientId == null ||
                (!clientId.startsWith("ignition-designer-") && !clientId.startsWith("gateway-web-ui"))) {
                LOGGER.warn("Session token request with invalid client_id: {}", clientId);
                return createErrorResponse("Invalid client_id");
            }

            long durationSeconds = 28800;

            // Generate CSRF token FIRST - this is critical for browser-based clients
            // Must succeed even if securityService is unavailable
            String httpSessionId = req.getRequest().getSession(true).getId();
            String csrfToken = generateCSRFToken(httpSessionId);
            LOGGER.debug("CSRF token generated for HTTP session: {}", httpSessionId);

            // Generate API session token (optional - may fail if securityService not ready)
            String apiToken = null;
            try {
                if (securityService != null) {
                    apiToken = securityService.generateApiToken(SecurityMode.DESIGNER_ADMIN, durationSeconds);
                }
            } catch (Exception e) {
                LOGGER.warn("Failed to generate API token (securityService issue), CSRF token still valid", e);
            }

            // Return tokens to client - CSRF token is always present
            JsonObject response = new JsonObject();
            response.addProperty("success", true);
            response.addProperty("token", csrfToken);
            if (apiToken != null) {
                response.addProperty("api_token", apiToken);
            }
            response.addProperty("expires_in", durationSeconds);
            response.addProperty("security_mode", SecurityMode.DESIGNER_ADMIN.toString());

            LOGGER.info("Session token created for client: {} (expires in {} seconds)", clientId, durationSeconds);

            // Audit log (v2.9.0 - session token creation)
            if (AUDIT_LOGGING_ENABLED && auditLogger != null) {
                try {
                    MessageDigest digest = MessageDigest.getInstance("SHA-256");
                    byte[] hash = digest.digest(clientId.getBytes(java.nio.charset.StandardCharsets.UTF_8));
                    StringBuilder hexString = new StringBuilder();
                    for (byte b : hash) {
                        String hex = Integer.toHexString(0xff & b);
                        if (hex.length() == 1) hexString.append('0');
                        hexString.append(hex);
                    }
                    String codeHash = hexString.toString();

                    Python3AuditEvent event = new Python3AuditEvent(
                            java.time.Instant.now(),
                            clientId,
                            req.getRequest().getRemoteAddr(),
                            SecurityMode.DESIGNER_ADMIN,
                            codeHash,
                            true,
                            0L,
                            null,
                            "REST:/auth/session"
                    );
                    auditLogger.logExecution(event);
                } catch (Exception e) {
                    LOGGER.warn("Failed to log session token creation audit event", e);
                }
            }

            return response;

        } catch (Exception e) {
            LOGGER.error("REST API: /auth/session failed", e);
            return createErrorResponse(e.getMessage());
        }
    }

    // Utility methods

    /**
     * Parse JSON from request body
     */
    private static JsonObject parseJsonBody(RequestContext req) throws IOException {
        HttpServletRequest httpRequest = req.getRequest();
        StringBuilder sb = new StringBuilder();
        BufferedReader reader = httpRequest.getReader();
        String line;
        while ((line = reader.readLine()) != null) {
            sb.append(line);
        }
        String jsonString = sb.toString();
        return JsonParser.parseString(jsonString).getAsJsonObject();
    }

    private static JsonObject createErrorResponse(String errorMessage) {
        JsonObject response = new JsonObject();
        response.addProperty("success", false);
        response.addProperty("error", errorMessage);
        return response;
    }

    private static JsonObject mapToJson(Map<String, Object> map) {
        JsonObject json = new JsonObject();
        for (Map.Entry<String, Object> entry : map.entrySet()) {
            Object value = entry.getValue();
            if (value instanceof String) {
                json.addProperty(entry.getKey(), (String) value);
            } else if (value instanceof Number) {
                json.addProperty(entry.getKey(), (Number) value);
            } else if (value instanceof Boolean) {
                json.addProperty(entry.getKey(), (Boolean) value);
            } else if (value != null) {
                json.addProperty(entry.getKey(), value.toString());
            }
        }
        return json;
    }

    /**
     * Handle GET /monitoring/metrics - Get enhanced monitoring metrics from Phase 2 Week 3-4
     * @since v2.14.0 Phase 2 Week 3-4
     */
    private static JsonObject handleGetEnhancedMetrics(RequestContext req, HttpServletResponse res) {
        LOGGER.debug("REST API: /monitoring/metrics called");

        try {
            if (scriptModule == null || scriptModule.getProcessPool() == null) {
                return createErrorResponse("Process pool not available");
            }

            Python3ProcessPool pool = scriptModule.getProcessPool();
            MetricsCollector collector = pool.getMetricsCollector();

            if (collector == null) {
                return createErrorResponse("Metrics collector not available");
            }

            JsonObject response = new JsonObject();
            response.addProperty("success", true);

            // Pool metrics
            response.addProperty("totalBorrows", collector.getTotalBorrows());
            response.addProperty("totalReturns", collector.getTotalReturns());
            response.addProperty("totalExecutions", collector.getTotalExecutions());
            response.addProperty("successfulExecutions", collector.getSuccessfulExecutions());
            response.addProperty("failedExecutions", collector.getFailedExecutions());
            response.addProperty("timeoutWaits", collector.getTimeoutWaits());

            // Response time percentiles
            response.addProperty("p50ResponseTimeMs", collector.getP50ResponseTime());
            response.addProperty("p95ResponseTimeMs", collector.getP95ResponseTime());
            response.addProperty("p99ResponseTimeMs", collector.getP99ResponseTime());

            // Error rate
            long total = collector.getTotalExecutions();
            long failed = collector.getFailedExecutions();
            double errorRate = total > 0 ? (double) failed / total * 100.0 : 0.0;
            response.addProperty("errorRatePercent", String.format("%.2f", errorRate));

            // Queue metrics
            response.addProperty("avgQueueWaitMs", collector.getAverageQueueWaitTimeMs());
            response.addProperty("maxQueueWaitMs", collector.getMaxQueueWaitTimeMs());

            LOGGER.debug("REST API: /monitoring/metrics completed successfully");
            return response;

        } catch (Exception e) {
            LOGGER.error("REST API: /monitoring/metrics failed", e);
            return createErrorResponse(e.getMessage());
        }
    }

    /**
     * Handle GET /monitoring/circuit-breaker - Get circuit breaker status
     * @since v2.14.0 Phase 2 Week 3-4
     */
    private static JsonObject handleGetCircuitBreakerStatus(RequestContext req, HttpServletResponse res) {
        LOGGER.debug("REST API: /monitoring/circuit-breaker called");

        try {
            if (scriptModule == null || scriptModule.getProcessPool() == null) {
                return createErrorResponse("Process pool not available");
            }

            Python3ProcessPool pool = scriptModule.getProcessPool();
            CircuitBreaker breaker = pool.getCircuitBreaker();

            if (breaker == null) {
                return createErrorResponse("Circuit breaker not available");
            }

            JsonObject response = new JsonObject();
            response.addProperty("success", true);
            response.addProperty("state", breaker.getState().toString());
            response.addProperty("failureCount", breaker.getFailureCount());
            response.addProperty("totalOpens", breaker.getTotalOpens());
            response.addProperty("totalCloses", breaker.getTotalCloses());
            response.addProperty("totalRejections", breaker.getTotalRejections());
            response.addProperty("timeSinceStateChangeMs", breaker.getTimeSinceStateChange());

            LOGGER.debug("REST API: /monitoring/circuit-breaker completed successfully");
            return response;

        } catch (Exception e) {
            LOGGER.error("REST API: /monitoring/circuit-breaker failed", e);
            return createErrorResponse(e.getMessage());
        }
    }

    /**
     * Handle GET /monitoring/alerts - Get alert manager status
     * @since v2.14.0 Phase 2 Week 3-4
     */
    private static JsonObject handleGetAlertManagerStatus(RequestContext req, HttpServletResponse res) {
        LOGGER.debug("REST API: /monitoring/alerts called");

        try {
            if (scriptModule == null || scriptModule.getProcessPool() == null) {
                return createErrorResponse("Process pool not available");
            }

            Python3ProcessPool pool = scriptModule.getProcessPool();
            AlertManager alertManager = pool.getAlertManager();

            if (alertManager == null) {
                return createErrorResponse("Alert manager not available");
            }

            JsonObject response = new JsonObject();
            response.addProperty("success", true);
            response.addProperty("totalAlerts", alertManager.getTotalAlerts());
            response.addProperty("suppressedAlerts", alertManager.getSuppressedAlerts());
            response.addProperty("alertSummary", alertManager.getAlertSummary());

            LOGGER.debug("REST API: /monitoring/alerts completed successfully");
            return response;

        } catch (Exception e) {
            LOGGER.error("REST API: /monitoring/alerts failed", e);
            return createErrorResponse(e.getMessage());
        }
    }

    /**
     * Handle GET /monitoring/prometheus - Get Prometheus metrics (NEW v2.15.0 Phase 3 Week 1-2)
     *
     * Returns metrics in Prometheus text format for scraping.
     * Response format: Plain text with Prometheus exposition format
     *
     * Example:
     * <pre>
     * # HELP python3_pool_size_total Total number of executors in the pool
     * # TYPE python3_pool_size_total gauge
     * python3_pool_size_total 3
     * </pre>
     */
    private static JsonObject handleGetPrometheusMetrics(RequestContext req, HttpServletResponse res) {
        LOGGER.debug("REST API: /monitoring/prometheus called");

        try {
            if (scriptModule == null || scriptModule.getProcessPool() == null) {
                // Return error in Prometheus format
                res.setContentType("text/plain; charset=utf-8");
                res.setStatus(HttpServletResponse.SC_SERVICE_UNAVAILABLE);
                res.getWriter().write("# ERROR: Process pool not available\n");
                res.getWriter().flush();
                return null;
            }

            Python3ProcessPool pool = scriptModule.getProcessPool();
            Python3ProcessPool.PoolStats stats = pool.getStats();
            MetricsCollector metricsCollector = pool.getMetricsCollector();

            // Export metrics in Prometheus format
            String prometheusMetrics = PrometheusExporter.exportMetrics(stats, metricsCollector);

            // Write plain text response
            res.setContentType("text/plain; version=0.0.4; charset=utf-8");
            res.setStatus(HttpServletResponse.SC_OK);
            res.getWriter().write(prometheusMetrics);
            res.getWriter().flush();

            LOGGER.debug("REST API: /monitoring/prometheus completed successfully");
            return null;  // Response already written

        } catch (Exception e) {
            LOGGER.error("REST API: /monitoring/prometheus failed", e);
            try {
                res.setContentType("text/plain; charset=utf-8");
                res.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                res.getWriter().write("# ERROR: " + e.getMessage() + "\n");
                res.getWriter().flush();
            } catch (Exception writeError) {
                LOGGER.error("Failed to write error response", writeError);
            }
            return null;
        }
    }

    private static Map<String, Object> jsonToMap(JsonObject json) {
        Map<String, Object> map = new HashMap<>();
        for (String key : json.keySet()) {
            map.put(key, jsonElementToObject(json.get(key)));
        }
        return map;
    }

    private static Object jsonElementToObject(JsonElement element) {
        if (element.isJsonPrimitive()) {
            if (element.getAsJsonPrimitive().isNumber()) {
                return element.getAsNumber();
            } else if (element.getAsJsonPrimitive().isBoolean()) {
                return element.getAsBoolean();
            } else {
                return element.getAsString();
            }
        } else if (element.isJsonNull()) {
            return null;
        } else {
            return element.toString();
        }
    }

    // =========================================================================
    // Python Distribution Management Handlers (v3.1.0)
    // =========================================================================

    /**
     * Handle GET /distributions - List all Python distributions with install status.
     *
     * Response: {"distributions": [...], "os": "linux", "installedCount": 1}
     */
    private static JsonObject handleGetDistributions(RequestContext req, HttpServletResponse res) {
        LOGGER.debug("REST API: /distributions called");

        try {
            if (distributionManager == null) {
                return createErrorResponse("Distribution manager not initialized");
            }

            JsonObject response = new JsonObject();
            JsonArray distributions = new JsonArray();

            for (PythonDistributionManager.VersionStatus status : distributionManager.getAllVersionStatuses()) {
                JsonObject dist = new JsonObject();
                dist.addProperty("version", status.version);
                dist.addProperty("fullVersion", status.fullVersion);
                dist.addProperty("installed", status.installed);
                dist.addProperty("available", status.available);
                if (status.pythonPath != null) {
                    dist.addProperty("pythonPath", status.pythonPath);
                }
                if (status.installSizeBytes > 0) {
                    dist.addProperty("installSizeBytes", status.installSizeBytes);
                    dist.addProperty("installSizeMB", status.installSizeBytes / (1024 * 1024));
                }

                // Include pool status if version has an active pool
                if (status.installed && poolManager != null && poolManager.isVersionAvailable(status.version)) {
                    dist.addProperty("poolActive", true);
                } else {
                    dist.addProperty("poolActive", false);
                }

                distributions.add(dist);
            }

            response.add("distributions", distributions);
            response.addProperty("os", distributionManager.detectOS());
            response.addProperty("installedCount", distributionManager.getInstalledVersions().size());
            response.addProperty("success", true);
            response.addProperty("timestamp", System.currentTimeMillis());

            return response;

        } catch (Exception e) {
            LOGGER.error("REST API: /distributions failed", e);
            return createErrorResponse(e.getMessage());
        }
    }

    /**
     * Handle POST /distributions/install - Install a Python version.
     *
     * Request body: {"version": "3.12"}
     * Response: {"success": true, "version": "3.12", "fullVersion": "3.12.1", "pythonPath": "..."}
     */
    private static JsonObject handleInstallDistribution(RequestContext req, HttpServletResponse res) {
        LOGGER.info("REST API: /distributions/install called");

        try {
            if (distributionManager == null) {
                return createErrorResponse("Distribution manager not initialized");
            }

            JsonObject requestJson = parseJsonBody(req);
            String version = requestJson.get("version").getAsString();

            if (version == null || version.trim().isEmpty()) {
                return createErrorResponse("Missing required field: version");
            }

            version = version.trim();
            LOGGER.info("Installing Python version: {}", version);

            distributionManager.installVersion(version);

            JsonObject response = new JsonObject();
            response.addProperty("success", true);
            response.addProperty("version", version);
            response.addProperty("fullVersion", distributionManager.getFullVersion(version));
            response.addProperty("pythonPath", distributionManager.getVersionExecutablePath(version));
            response.addProperty("message", "Python " + version + " installed successfully");
            response.addProperty("timestamp", System.currentTimeMillis());

            return response;

        } catch (IllegalArgumentException e) {
            LOGGER.warn("REST API: /distributions/install - invalid version: {}", e.getMessage());
            return createErrorResponse(e.getMessage());
        } catch (Exception e) {
            LOGGER.error("REST API: /distributions/install failed", e);
            return createErrorResponse("Installation failed: " + e.getMessage());
        }
    }

    /**
     * Handle POST /distributions/uninstall - Uninstall a Python version.
     *
     * Request body: {"version": "3.12"}
     * Response: {"success": true, "version": "3.12", "message": "..."}
     */
    private static JsonObject handleUninstallDistribution(RequestContext req, HttpServletResponse res) {
        LOGGER.info("REST API: /distributions/uninstall called");

        try {
            if (distributionManager == null) {
                return createErrorResponse("Distribution manager not initialized");
            }

            JsonObject requestJson = parseJsonBody(req);
            String version = requestJson.get("version").getAsString();

            if (version == null || version.trim().isEmpty()) {
                return createErrorResponse("Missing required field: version");
            }

            version = version.trim();

            // Check if this version has an active pool - warn but allow uninstall
            if (poolManager != null && poolManager.isVersionAvailable(version)) {
                LOGGER.warn("Uninstalling Python {} which has an active pool. Pool will become invalid.", version);
            }

            LOGGER.info("Uninstalling Python version: {}", version);
            distributionManager.uninstallVersion(version);

            JsonObject response = new JsonObject();
            response.addProperty("success", true);
            response.addProperty("version", version);
            response.addProperty("message", "Python " + version + " uninstalled successfully. Restart module to update pools.");
            response.addProperty("timestamp", System.currentTimeMillis());

            return response;

        } catch (Exception e) {
            LOGGER.error("REST API: /distributions/uninstall failed", e);
            return createErrorResponse("Uninstall failed: " + e.getMessage());
        }
    }
}
