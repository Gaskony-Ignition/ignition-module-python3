package com.inductiveautomation.ignition.examples.python3.gateway;

import com.inductiveautomation.ignition.gateway.dataroutes.RequestContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Security service for Python 3 code execution.
 * Handles authentication, authorization, and security mode determination.
 *
 * Security Model:
 * - DESIGNER_ADMIN: Designer IDE users (trusted, authenticated via Designer login)
 * - ADMIN: REST API users with admin key (requires HTTPS)
 * - RESTRICTED: Unauthenticated REST API users (safe modules only)
 *
 * @since v2.6.0
 */
public class Python3SecurityService {
    private static final Logger LOGGER = LoggerFactory.getLogger(Python3SecurityService.class);

    private final GatewayHook gatewayHook;
    private final Map<String, ApiToken> activeTokens = new ConcurrentHashMap<>();
    private String adminApiKey;

    /**
     * API token data structure.
     */
    private static class ApiToken {
        final String token;
        final SecurityMode securityMode;
        final Instant createdAt;
        final Instant expiresAt;

        ApiToken(String token, SecurityMode securityMode, Instant createdAt, Instant expiresAt) {
            this.token = token;
            this.securityMode = securityMode;
            this.createdAt = createdAt;
            this.expiresAt = expiresAt;
        }

        boolean isExpired() {
            return Instant.now().isAfter(expiresAt);
        }
    }

    public Python3SecurityService(GatewayHook gatewayHook) {
        this.gatewayHook = gatewayHook;
        loadConfiguration();
    }

    /**
     * Load security configuration from system properties.
     */
    private void loadConfiguration() {
        adminApiKey = System.getProperty("ignition.python3.admin.apikey");

        if (adminApiKey != null) {
            if (adminApiKey.length() < 32) {
                LOGGER.error("CRITICAL SECURITY ERROR: Admin API key is too short ({} chars).", adminApiKey.length());
                LOGGER.error("Minimum 32 characters required. Current key is INSECURE!");
                LOGGER.error("Generate a secure key: openssl rand -hex 32");
                throw new IllegalStateException(
                    "Admin API key must be at least 32 characters. Current: " + adminApiKey.length()
                );
            }

            LOGGER.info("Admin API key configured (length: {} chars)", adminApiKey.length());
            LOGGER.info("ADMIN mode available via: Authorization: Bearer <admin-key>");
            LOGGER.warn("ADMIN mode should ONLY be used over HTTPS!");
        } else {
            LOGGER.info("No admin API key configured. ADMIN mode unavailable via REST API.");
            LOGGER.info("Designer IDE users still have full DESIGNER_ADMIN capabilities.");
        }
    }

    /**
     * Determine security mode for a request.
     * <p>
     * Decision flow:
     * 1. Check if Designer IDE request → DESIGNER_ADMIN (trusted)
     * 2. Check for admin API key → ADMIN
     * 3. Check for valid API token → Token's security mode
     * 4. No authentication → RESTRICTED (safe modules only)
     *
     * @param req The request context
     * @return The security mode to use
     */
    public SecurityMode determineSecurityMode(RequestContext req) {
        // 1. Check if Designer IDE request (trusted, no token needed)
        String userAgent = req.getRequest().getHeader("User-Agent");
        if (userAgent != null && userAgent.toLowerCase().contains("ignition-designer")) {
            LOGGER.debug("Designer IDE request detected - granting DESIGNER_ADMIN mode");
            return SecurityMode.DESIGNER_ADMIN;
        }

        // 2. REST API request - check for admin API key
        String authHeader = req.getRequest().getHeader("Authorization");
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            String token = authHeader.substring(7);
            try {
                return validateApiToken(token);
            } catch (SecurityException e) {
                LOGGER.warn("Invalid API token: {}", e.getMessage());
                // Fall through to RESTRICTED mode
            }
        }

        // 3. Legacy support: Check X-Python3-Admin-Key header
        String adminKeyHeader = req.getRequest().getHeader("X-Python3-Admin-Key");
        if (adminKeyHeader != null && isValidAdminKey(adminKeyHeader)) {
            LOGGER.debug("Valid admin key provided via X-Python3-Admin-Key header - granting ADMIN mode");
            return SecurityMode.ADMIN;
        }

        // 4. No authentication - RESTRICTED mode (safe modules only)
        LOGGER.debug("No authentication provided - using RESTRICTED mode");
        return SecurityMode.RESTRICTED;
    }

    /**
     * Validate API token for REST API access.
     *
     * @param token The API token to validate
     * @return SecurityMode granted (RESTRICTED or ADMIN)
     * @throws SecurityException if token is invalid
     */
    public SecurityMode validateApiToken(String token) throws SecurityException {
        if (token == null || token.trim().isEmpty()) {
            throw new SecurityException("API token required");
        }

        // Check if token is the admin API key
        if (isValidAdminKey(token)) {
            LOGGER.debug("Admin API key validated - granting ADMIN mode");
            return SecurityMode.ADMIN;
        }

        // Check if token is in active tokens map
        ApiToken apiToken = activeTokens.get(token);
        if (apiToken != null) {
            if (apiToken.isExpired()) {
                activeTokens.remove(token);
                throw new SecurityException("API token has expired");
            }
            LOGGER.debug("API token validated - granting {} mode", apiToken.securityMode);
            return apiToken.securityMode;
        }

        throw new SecurityException("Invalid API token");
    }

    /**
     * Check if provided key matches the configured admin API key.
     * Uses constant-time comparison to prevent timing attacks.
     *
     * @param providedKey The key to validate
     * @return true if valid admin key
     */
    private boolean isValidAdminKey(String providedKey) {
        if (adminApiKey == null || providedKey == null) {
            return false;
        }

        // Constant-time comparison to prevent timing attacks
        try {
            return MessageDigest.isEqual(
                providedKey.getBytes(StandardCharsets.UTF_8),
                adminApiKey.getBytes(StandardCharsets.UTF_8)
            );
        } catch (Exception e) {
            LOGGER.error("Error validating admin key", e);
            return false;
        }
    }

    /**
     * Generate a new API token (admin only).
     * <p>
     * In production, this would be called from an authenticated admin endpoint.
     * For now, it's a utility method for testing.
     *
     * @param securityMode The security mode to grant
     * @param durationSeconds Token lifetime in seconds
     * @return The generated token
     */
    public String generateApiToken(SecurityMode securityMode, long durationSeconds) {
        // Generate random token
        String token = java.util.UUID.randomUUID().toString();

        Instant now = Instant.now();
        Instant expiresAt = now.plusSeconds(durationSeconds);

        ApiToken apiToken = new ApiToken(token, securityMode, now, expiresAt);
        activeTokens.put(token, apiToken);

        LOGGER.info("Generated API token with {} mode (expires in {} seconds)", securityMode, durationSeconds);

        return token;
    }

    /**
     * Revoke an API token.
     *
     * @param token The token to revoke
     * @return true if token was revoked, false if not found
     */
    public boolean revokeApiToken(String token) {
        ApiToken removed = activeTokens.remove(token);
        if (removed != null) {
            LOGGER.info("Revoked API token with {} mode", removed.securityMode);
            return true;
        }
        return false;
    }

    /**
     * Check if HTTPS is required for the given security mode.
     *
     * @param securityMode The security mode
     * @param req The request context
     * @throws SecurityException if HTTPS is required but not used
     */
    public void enforceHttpsRequirement(SecurityMode securityMode, RequestContext req) throws SecurityException {
        // ADMIN mode requires HTTPS (unless disabled for development)
        if (securityMode == SecurityMode.ADMIN) {
            boolean requireHttps = Boolean.parseBoolean(
                System.getProperty("ignition.python3.admin.requirehttps", "true")
            );

            if (requireHttps && !req.getRequest().isSecure()) {
                throw new SecurityException(
                    "ADMIN mode requires HTTPS. Use 'https://' or disable with -Dignition.python3.admin.requirehttps=false (NOT recommended for production)"
                );
            }
        }
    }

    /**
     * Get the admin API key (for testing purposes only).
     * DO NOT expose this in production API.
     *
     * @return The admin API key or null if not configured
     */
    String getAdminApiKey() {
        return adminApiKey;
    }

    /**
     * Get count of active tokens (for metrics).
     *
     * @return Number of active tokens
     */
    public int getActiveTokenCount() {
        // Clean up expired tokens
        activeTokens.entrySet().removeIf(entry -> entry.getValue().isExpired());
        return activeTokens.size();
    }
}
