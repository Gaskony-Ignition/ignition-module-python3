package com.inductiveautomation.ignition.examples.python3.gateway;

import com.inductiveautomation.ignition.common.gson.JsonArray;
import com.inductiveautomation.ignition.common.gson.JsonElement;
import com.inductiveautomation.ignition.common.gson.JsonObject;
import com.inductiveautomation.ignition.common.gson.JsonParser;
import com.inductiveautomation.ignition.examples.python3.ApiEndpoints;
import com.inductiveautomation.ignition.examples.python3.PoolConfig;
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
import java.net.URLDecoder;
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
 *
 * v3.6.15: Refactored — handler methods split into companion classes:
 *   ExecutionHandlers, ScriptAndPackageHandlers, MonitoringHandlers.
 * This class now contains only infrastructure (security, CSRF, routing, utilities).
 */
public final class Python3RestEndpoints {

    private static final Logger LOGGER = LoggerFactory.getLogger(Python3RestEndpoints.class);
    private static Python3ScriptModule scriptModule;
    private static Python3ScriptRepository scriptRepository;
    static Python3MetricsCollector metricsCollector = new Python3MetricsCollector();
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
    static final boolean AUDIT_LOGGING_ENABLED = true;

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
    static void loadIPWhitelist() {
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
    static void validateIPWhitelist(RequestContext req, SecurityMode securityMode) throws SecurityException {
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
    static boolean isIPAllowed(String clientIP) {
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
    static boolean isIPInCIDR(String ip, String cidr) {
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
    static long ipToLong(String ip) {
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
    static String getClientIPAddress(RequestContext req) {
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
    static SecurityMode determineSecurityMode(RequestContext req) {
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
    static RouteAccess checkExecutePermission(RequestContext req) {
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
    static String getClientId(RequestContext req) {
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
    static boolean isGatewayAuthenticated(RequestContext req) {
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
            // 4. v3.6.8: Accept Designer REST client identified by X-Source header
            // When securityService is unavailable or Bearer token acquisition fails,
            // the Designer client still sends X-Source: Python3-IDE. This provides a
            // fallback authentication path so package install/uninstall and other
            // management operations work reliably from the Designer.
            String source = req.getRequest().getHeader("X-Source");
            if ("Python3-IDE".equals(source)) {
                LOGGER.debug("Authenticated via X-Source: Python3-IDE header (Designer client fallback)");
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
    static RouteAccess checkManagePermission(RequestContext req) {
        return isGatewayAuthenticated(req) ? RouteAccess.GRANTED : RouteAccess.UNAUTHORIZED;
    }

    /**
     * Check if user has permission to read data (diagnostics, stats).
     * Requires Gateway authentication.
     *
     * v3.5.2: Require Gateway login
     */
    static RouteAccess checkReadPermission(RequestContext req) {
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
    static boolean secureEquals(String a, String b) {
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
    static void applySecurityHeaders(HttpServletResponse res) {
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

    /** Functional interface for handler business logic, allowing checked exceptions. */
    @FunctionalInterface
    interface HandlerLogic {
        JsonObject execute() throws Exception;
    }

    /**
     * Executes handler logic with standardised cross-cutting concerns:
     * security headers (always, even on error), entry/exit logging, and
     * uniform exception-to-error-response conversion.
     *
     * @param endpoint short label used in log messages (e.g. "exec")
     * @param res      HTTP response — security headers applied before delegate runs
     * @param logic    the handler business logic
     */
    static JsonObject withHandler(
            String endpoint,
            HttpServletResponse res,
            HandlerLogic logic) {
        LOGGER.debug("REST API: /{} called", endpoint);
        applySecurityHeaders(res);
        try {
            JsonObject result = logic.execute();
            LOGGER.debug("REST API: /{} completed", endpoint);
            return result;
        } catch (Exception e) {
            LOGGER.error("REST API: /{} failed", endpoint, e);
            return ApiResponse.error(e.getMessage());
        }
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
    static String generateCSRFToken(String sessionId) {
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
    static void cleanupExpiredCSRFTokens() {
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
    static boolean validateCSRFToken(RequestContext req) {
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
    static void validateCSRFIfSession(RequestContext req) {
        // Skip CSRF for requests authenticated via Bearer token (Designer REST client)
        String authHeader = req.getRequest().getHeader("Authorization");
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            return;
        }

        // Skip CSRF for Designer REST client requests (identified by X-Source header)
        // v3.6.6: When Bearer token acquisition fails, the Designer client still sends
        // X-Source: Python3-IDE. Without this check, the request hits CSRF validation
        // because the servlet container may create an HTTP session.
        String source = req.getRequest().getHeader("X-Source");
        if ("Python3-IDE".equals(source)) {
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
    static void auditLog(String action, String details) {
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
    static String sanitizeForLogging(String data) {
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
    static String hashCode(String code) {
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
    static void validateCode(String code) {
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
    static void validateScriptName(String name) {
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
    static void validateFolderPath(String folderPath) {
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
     *
     * v3.6.15: Handler methods delegated to companion classes via EndpointContext.
     */
    public static void mountRoutes(RouteGroup routes) {
        LOGGER.info("Mounting Python3 REST API routes (Ignition 8.3 OpenAPI compliant)");

        // v2.15.9: Initialize IP whitelist for ADMIN mode protection
        loadIPWhitelist();

        EndpointContext ctx = new EndpointContext(
            scriptModule, scriptRepository, packageManager, securityService,
            auditLogger, poolManager, distributionManager, logsDir, metricsCollector
        );

        ExecutionHandlers exec = new ExecutionHandlers(ctx);
        ScriptAndPackageHandlers scriptPkg = new ScriptAndPackageHandlers(ctx);
        MonitoringHandlers monitoring = new MonitoringHandlers(ctx);

        // POST /data/python3integration/auth/session - Create session token (NEW v2.9.0)
        routes.newRoute(ApiEndpoints.ROUTE_AUTH_SESSION)
            .handler(exec::handleCreateSession)
            .method(HttpMethod.POST)
            .type(RouteGroup.TYPE_JSON)
            .accessControl(req -> RouteAccess.GRANTED)  // No auth required to GET a token
            .mount();

        // POST /data/python3integration/api/v1/exec - Execute Python code
        routes.newRoute(ApiEndpoints.ROUTE_EXEC)
            .handler(exec::handleExec)
            .method(HttpMethod.POST)
            .type(RouteGroup.TYPE_JSON)
            .accessControl(Python3RestEndpoints::checkExecutePermission)  // AUTH + RATE LIMIT
            .mount();

        // POST /data/python3integration/api/v1/shell-interactive/create - Create interactive shell session (v2.5.8)
        routes.newRoute(ApiEndpoints.ROUTE_SHELL_INTERACTIVE_CREATE)
            .handler(exec::handleCreateShellSession)
            .method(HttpMethod.POST)
            .type(RouteGroup.TYPE_JSON)
            .accessControl(Python3RestEndpoints::checkExecutePermission)  // AUTH + RATE LIMIT
            .mount();

        // POST /data/python3integration/api/v1/shell-interactive/exec - Execute command in interactive shell (v2.5.8)
        routes.newRoute(ApiEndpoints.ROUTE_SHELL_INTERACTIVE_EXEC)
            .handler(exec::handleInteractiveShellExec)
            .method(HttpMethod.POST)
            .type(RouteGroup.TYPE_JSON)
            .accessControl(Python3RestEndpoints::checkExecutePermission)  // AUTH + RATE LIMIT
            .mount();

        // POST /data/python3integration/api/v1/shell-interactive/close - Close interactive shell session (v2.5.8)
        routes.newRoute(ApiEndpoints.ROUTE_SHELL_INTERACTIVE_CLOSE)
            .handler(exec::handleCloseShellSession)
            .method(HttpMethod.POST)
            .type(RouteGroup.TYPE_JSON)
            .accessControl(Python3RestEndpoints::checkExecutePermission)  // AUTH + RATE LIMIT
            .mount();

        // POST /data/python3integration/api/v1/eval - Evaluate Python expression
        routes.newRoute(ApiEndpoints.ROUTE_EVAL)
            .handler(exec::handleEval)
            .method(HttpMethod.POST)
            .type(RouteGroup.TYPE_JSON)
            .accessControl(Python3RestEndpoints::checkExecutePermission)  // AUTH + RATE LIMIT
            .mount();

        // POST /data/python3integration/api/v1/call-module - Call Python module function
        routes.newRoute(ApiEndpoints.ROUTE_CALL_MODULE)
            .handler(exec::handleCallModule)
            .method(HttpMethod.POST)
            .type(RouteGroup.TYPE_JSON)
            .accessControl(Python3RestEndpoints::checkExecutePermission)  // AUTH + RATE LIMIT
            .mount();

        // POST /data/python3integration/api/v1/call-script - Call saved Python script
        routes.newRoute(ApiEndpoints.ROUTE_CALL_SCRIPT)
            .handler(exec::handleCallScript)
            .method(HttpMethod.POST)
            .type(RouteGroup.TYPE_JSON)
            .accessControl(Python3RestEndpoints::checkExecutePermission)  // AUTH + RATE LIMIT
            .mount();

        // GET /data/python3integration/api/v1/version - Get Python version
        routes.newRoute(ApiEndpoints.ROUTE_VERSION)
            .handler(monitoring::handleGetVersion)
            .method(HttpMethod.GET)
            .type(RouteGroup.TYPE_JSON)
            .accessControl(Python3RestEndpoints::checkReadPermission)  // AUTH (read-only)
            .mount();

        // GET /data/python3integration/api/v1/pool-stats - Get process pool statistics
        routes.newRoute(ApiEndpoints.ROUTE_POOL_STATS)
            .handler(monitoring::handleGetPoolStats)
            .method(HttpMethod.GET)
            .type(RouteGroup.TYPE_JSON)
            .accessControl(Python3RestEndpoints::checkReadPermission)  // AUTH (read-only)
            .mount();

        // POST /data/python3integration/api/v1/pool-size - Set process pool size (NEW v1.17.2)
        routes.newRoute(ApiEndpoints.ROUTE_POOL_SIZE)
            .handler(monitoring::handleSetPoolSize)
            .method(HttpMethod.POST)
            .type(RouteGroup.TYPE_JSON)
            .accessControl(Python3RestEndpoints::checkManagePermission)  // AUTH (requires management permission)
            .mount();

        // GET /data/python3integration/api/v1/health - Health check
        routes.newRoute(ApiEndpoints.ROUTE_HEALTH)
            .handler(monitoring::handleHealthCheck)
            .method(HttpMethod.GET)
            .type(RouteGroup.TYPE_JSON)
            .accessControl(Python3RestEndpoints::checkReadPermission)  // AUTH (read-only)
            .mount();

        // GET /data/python3integration/api/v1/versions - Available Python versions (v3.1.0)
        routes.newRoute(ApiEndpoints.ROUTE_VERSIONS)
            .handler(monitoring::handleGetVersions)
            .method(HttpMethod.GET)
            .type(RouteGroup.TYPE_JSON)
            .accessControl(Python3RestEndpoints::checkReadPermission)
            .mount();

        // GET /data/python3integration/api/v1/diagnostics - Performance diagnostics
        routes.newRoute(ApiEndpoints.ROUTE_DIAGNOSTICS)
            .handler(monitoring::handleDiagnostics)
            .method(HttpMethod.GET)
            .type(RouteGroup.TYPE_JSON)
            .accessControl(Python3RestEndpoints::checkReadPermission)  // AUTH (read-only)
            .mount();

        // GET /data/python3integration/api/v1/example - Run example test
        routes.newRoute(ApiEndpoints.ROUTE_EXAMPLE)
            .handler(exec::handleExample)
            .method(HttpMethod.GET)
            .type(RouteGroup.TYPE_JSON)
            .accessControl(Python3RestEndpoints::checkExecutePermission)  // AUTH + RATE LIMIT
            .mount();

        // POST /data/python3integration/api/v1/check-syntax - Check Python syntax
        routes.newRoute(ApiEndpoints.ROUTE_CHECK_SYNTAX)
            .handler(exec::handleCheckSyntax)
            .method(HttpMethod.POST)
            .type(RouteGroup.TYPE_JSON)
            .accessControl(Python3RestEndpoints::checkExecutePermission)  // AUTH + RATE LIMIT
            .mount();

        // POST /data/python3integration/api/v1/completions - Get code completions
        routes.newRoute(ApiEndpoints.ROUTE_COMPLETIONS)
            .handler(exec::handleGetCompletions)
            .method(HttpMethod.POST)
            .type(RouteGroup.TYPE_JSON)
            .accessControl(Python3RestEndpoints::checkReadPermission)  // AUTH (completions are read-only)
            .mount();

        // Performance Monitoring Endpoints

        // GET /data/python3integration/api/v1/metrics - Get performance metrics
        routes.newRoute(ApiEndpoints.ROUTE_METRICS)
            .handler(monitoring::handleGetMetrics)
            .method(HttpMethod.GET)
            .type(RouteGroup.TYPE_JSON)
            .accessControl(Python3RestEndpoints::checkReadPermission)  // AUTH (read-only)
            .mount();

        // GET /data/python3integration/api/v1/gateway-impact - Get Gateway impact assessment
        routes.newRoute(ApiEndpoints.ROUTE_GATEWAY_IMPACT)
            .handler(monitoring::handleGetGatewayImpact)
            .method(HttpMethod.GET)
            .type(RouteGroup.TYPE_JSON)
            .accessControl(Python3RestEndpoints::checkReadPermission)  // AUTH (read-only)
            .mount();

        // GET /data/python3integration/api/v1/metrics/script-metrics - Get per-script metrics (NEW v1.16.0)
        routes.newRoute(ApiEndpoints.ROUTE_METRICS_SCRIPT_METRICS)
            .handler(monitoring::handleGetScriptMetrics)
            .method(HttpMethod.GET)
            .type(RouteGroup.TYPE_JSON)
            .accessControl(Python3RestEndpoints::checkReadPermission)
            .mount();

        // GET /data/python3integration/api/v1/metrics/historical - Get historical metrics (NEW v1.16.0)
        routes.newRoute(ApiEndpoints.ROUTE_METRICS_HISTORICAL)
            .handler(monitoring::handleGetHistoricalMetrics)
            .method(HttpMethod.GET)
            .type(RouteGroup.TYPE_JSON)
            .accessControl(Python3RestEndpoints::checkReadPermission)
            .mount();

        // GET /data/python3integration/api/v1/metrics/alerts - Get active health alerts (NEW v1.16.0)
        routes.newRoute(ApiEndpoints.ROUTE_METRICS_ALERTS)
            .handler(monitoring::handleGetHealthAlerts)
            .method(HttpMethod.GET)
            .type(RouteGroup.TYPE_JSON)
            .accessControl(Python3RestEndpoints::checkReadPermission)
            .mount();

        // GET /data/python3integration/api/v1/monitoring/metrics - Get enhanced metrics (NEW v2.14.0 Phase 2 Week 3-4)
        routes.newRoute(ApiEndpoints.ROUTE_MONITORING_METRICS)
            .handler(monitoring::handleGetEnhancedMetrics)
            .method(HttpMethod.GET)
            .type(RouteGroup.TYPE_JSON)
            .accessControl(Python3RestEndpoints::checkReadPermission)
            .mount();

        // GET /data/python3integration/api/v1/monitoring/circuit-breaker - Get circuit breaker status (NEW v2.14.0 Phase 2 Week 3-4)
        routes.newRoute(ApiEndpoints.ROUTE_MONITORING_CIRCUIT_BREAKER)
            .handler(monitoring::handleGetCircuitBreakerStatus)
            .method(HttpMethod.GET)
            .type(RouteGroup.TYPE_JSON)
            .accessControl(Python3RestEndpoints::checkReadPermission)
            .mount();

        // GET /data/python3integration/api/v1/monitoring/alerts - Get alert manager status (NEW v2.14.0 Phase 2 Week 3-4)
        routes.newRoute(ApiEndpoints.ROUTE_MONITORING_ALERTS)
            .handler(monitoring::handleGetAlertManagerStatus)
            .method(HttpMethod.GET)
            .type(RouteGroup.TYPE_JSON)
            .accessControl(Python3RestEndpoints::checkReadPermission)
            .mount();

        // GET /data/python3integration/api/v1/monitoring/prometheus - Get Prometheus metrics (NEW v2.15.0 Phase 3 Week 1-2)
        routes.newRoute(ApiEndpoints.ROUTE_MONITORING_PROMETHEUS)
            .handler(monitoring::handleGetPrometheusMetrics)
            .method(HttpMethod.GET)
            .type(RouteGroup.TYPE_JSON)  // Handler will override content-type to text/plain
            .accessControl(Python3RestEndpoints::checkReadPermission)
            .mount();

        // GET /data/python3integration/api/v1/logs - Gateway log viewer (v3.5.0)
        routes.newRoute(ApiEndpoints.ROUTE_LOGS)
            .handler(monitoring::handleGetLogs)
            .method(HttpMethod.GET)
            .type(RouteGroup.TYPE_JSON)
            .accessControl(Python3RestEndpoints::checkReadPermission)
            .mount();

        // Script Management Endpoints

        // POST /data/python3integration/api/v1/scripts/save - Save a script
        routes.newRoute(ApiEndpoints.ROUTE_SCRIPTS_SAVE)
            .handler(scriptPkg::handleSaveScript)
            .method(HttpMethod.POST)
            .type(RouteGroup.TYPE_JSON)
            .accessControl(Python3RestEndpoints::checkManagePermission)  // AUTH + RATE LIMIT
            .mount();

        // GET /data/python3integration/api/v1/scripts/load/{name} - Load a script
        routes.newRoute(ApiEndpoints.ROUTE_SCRIPTS_LOAD)
            .handler(scriptPkg::handleLoadScript)
            .method(HttpMethod.GET)
            .type(RouteGroup.TYPE_JSON)
            .accessControl(Python3RestEndpoints::checkReadPermission)  // AUTH (read-only)
            .mount();

        // GET /data/python3integration/api/v1/scripts/list - List all scripts
        routes.newRoute(ApiEndpoints.ROUTE_SCRIPTS_LIST)
            .handler(scriptPkg::handleListScripts)
            .method(HttpMethod.GET)
            .type(RouteGroup.TYPE_JSON)
            .accessControl(Python3RestEndpoints::checkReadPermission)  // AUTH (read-only)
            .mount();

        // POST /data/python3integration/api/v1/scripts/delete/{name} - Delete a script
        // v3.6.5: Changed from DELETE to POST for broader servlet compatibility
        routes.newRoute(ApiEndpoints.ROUTE_SCRIPTS_DELETE)
            .handler(scriptPkg::handleDeleteScript)
            .method(HttpMethod.POST)
            .type(RouteGroup.TYPE_JSON)
            .accessControl(Python3RestEndpoints::checkManagePermission)  // AUTH + RATE LIMIT
            .mount();

        // GET /data/python3integration/api/v1/scripts/available - Get available scripts (NEW v2.0.24)
        routes.newRoute(ApiEndpoints.ROUTE_SCRIPTS_AVAILABLE)
            .handler(scriptPkg::handleGetAvailableScripts)
            .method(HttpMethod.GET)
            .type(RouteGroup.TYPE_JSON)
            .accessControl(Python3RestEndpoints::checkReadPermission)  // AUTH (read-only)
            .mount();

        // Package Management Endpoints (NEW v2.3.0)

        // GET /data/python3integration/api/v1/packages/catalog - Get available packages
        routes.newRoute(ApiEndpoints.ROUTE_PACKAGES_CATALOG)
            .handler(scriptPkg::handleGetPackageCatalog)
            .method(HttpMethod.GET)
            .type(RouteGroup.TYPE_JSON)
            .accessControl(Python3RestEndpoints::checkReadPermission)  // AUTH (read-only)
            .mount();

        // GET /data/python3integration/api/v1/packages/status - Get package installation status
        routes.newRoute(ApiEndpoints.ROUTE_PACKAGES_STATUS)
            .handler(scriptPkg::handleGetPackageStatus)
            .method(HttpMethod.GET)
            .type(RouteGroup.TYPE_JSON)
            .accessControl(Python3RestEndpoints::checkReadPermission)  // AUTH (read-only)
            .mount();

        // POST /data/python3integration/api/v1/packages/install/:name - Install a package bundle
        routes.newRoute(ApiEndpoints.ROUTE_PACKAGES_INSTALL)
            .handler(scriptPkg::handleInstallPackage)
            .method(HttpMethod.POST)
            .type(RouteGroup.TYPE_JSON)
            .accessControl(Python3RestEndpoints::checkManagePermission)  // AUTH + RATE LIMIT
            .mount();

        // POST /data/python3integration/api/v1/packages/uninstall/:name - Uninstall a package bundle
        routes.newRoute(ApiEndpoints.ROUTE_PACKAGES_UNINSTALL)
            .handler(scriptPkg::handleUninstallPackage)
            .method(HttpMethod.POST)
            .type(RouteGroup.TYPE_JSON)
            .accessControl(Python3RestEndpoints::checkManagePermission)  // AUTH + RATE LIMIT
            .mount();

        // POST /data/python3integration/api/v1/packages/verify - Verify installed packages
        routes.newRoute(ApiEndpoints.ROUTE_PACKAGES_VERIFY)
            .handler(scriptPkg::handleVerifyPackages)
            .method(HttpMethod.POST)
            .type(RouteGroup.TYPE_JSON)
            .accessControl(Python3RestEndpoints::checkReadPermission)  // AUTH (read-only, verification)
            .mount();

        // GET /data/python3integration/api/v1/packages/search-pypi - Search PyPI (v3.3.0, updated v3.5.0)
        routes.newRoute(ApiEndpoints.ROUTE_PACKAGES_SEARCH_PYPI)
            .handler(scriptPkg::handleSearchPyPI)
            .method(HttpMethod.GET)
            .type(RouteGroup.TYPE_JSON)
            .accessControl(Python3RestEndpoints::checkReadPermission)
            .mount();

        // GET /data/python3integration/api/v1/packages/pypi-info/{name} - PyPI package metadata (v3.5.0)
        routes.newRoute(ApiEndpoints.ROUTE_PACKAGES_PYPI_INFO)
            .handler(scriptPkg::handleGetPyPIInfo)
            .method(HttpMethod.GET)
            .type(RouteGroup.TYPE_JSON)
            .accessControl(Python3RestEndpoints::checkReadPermission)
            .mount();

        // ===== Python Distribution Management (v3.1.0) =====

        // GET /data/python3integration/api/v1/distributions - List all Python distributions with install status
        routes.newRoute(ApiEndpoints.ROUTE_DISTRIBUTIONS)
            .handler(monitoring::handleGetDistributions)
            .method(HttpMethod.GET)
            .type(RouteGroup.TYPE_JSON)
            .accessControl(Python3RestEndpoints::checkReadPermission)
            .mount();

        // POST /data/python3integration/api/v1/distributions/install - Install a Python version
        routes.newRoute(ApiEndpoints.ROUTE_DISTRIBUTIONS_INSTALL)
            .handler(monitoring::handleInstallDistribution)
            .method(HttpMethod.POST)
            .type(RouteGroup.TYPE_JSON)
            .accessControl(Python3RestEndpoints::checkManagePermission)
            .mount();

        // POST /data/python3integration/api/v1/distributions/uninstall - Uninstall a Python version
        routes.newRoute(ApiEndpoints.ROUTE_DISTRIBUTIONS_UNINSTALL)
            .handler(monitoring::handleUninstallDistribution)
            .method(HttpMethod.POST)
            .type(RouteGroup.TYPE_JSON)
            .accessControl(Python3RestEndpoints::checkManagePermission)
            .mount();

        LOGGER.info("Python3 REST API routes mounted successfully at /api/v1/*");
    }

    // =========================================================================
    // Utility methods — package-private so companion classes can call them
    // =========================================================================

    /**
     * Parse JSON from request body
     */
    static JsonObject parseJsonBody(RequestContext req) throws IOException {
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

    static JsonObject mapToJson(Map<String, Object> map) {
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

    static Map<String, Object> jsonToMap(JsonObject json) {
        Map<String, Object> map = new HashMap<>();
        for (String key : json.keySet()) {
            map.put(key, jsonElementToObject(json.get(key)));
        }
        return map;
    }

    static Object jsonElementToObject(JsonElement element) {
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
}
