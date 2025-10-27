package com.inductiveautomation.ignition.examples.python3.gateway;

import com.inductiveautomation.ignition.gateway.dataroutes.RequestContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.Base64;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

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
    private final SecureRandom secureRandom = new SecureRandom();
    private byte[] tokenSigningKey;

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
            LOGGER.info("Designer IDE users can obtain session tokens via /auth/session endpoint.");
        }

        // Generate cryptographically secure signing key for session tokens
        tokenSigningKey = new byte[32]; // 256-bit key
        secureRandom.nextBytes(tokenSigningKey);
        LOGGER.info("Session token signing key initialized (256-bit)");
    }

    /**
     * Determine security mode for a request.
     * <p>
     * Decision flow:
     * 1. Check for valid session token (Designer IDE or API) → Token's security mode
     * 2. Check for admin API key → ADMIN
     * 3. Check for legacy X-Python3-Admin-Key → ADMIN
     * 4. No authentication → RESTRICTED (safe modules only)
     *
     * @param req The request context
     * @return The security mode to use
     */
    public SecurityMode determineSecurityMode(RequestContext req) {
        // 1. Check for Bearer token (session tokens or admin API key)
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

        // 2. Legacy support: Check X-Python3-Admin-Key header
        String adminKeyHeader = req.getRequest().getHeader("X-Python3-Admin-Key");
        if (adminKeyHeader != null && isValidAdminKey(adminKeyHeader)) {
            LOGGER.debug("Valid admin key provided via X-Python3-Admin-Key header - granting ADMIN mode");
            return SecurityMode.ADMIN;
        }

        // 3. No authentication - RESTRICTED mode (safe modules only)
        LOGGER.debug("No authentication provided - using RESTRICTED mode");
        return SecurityMode.RESTRICTED;
    }

    /**
     * Validate API token for REST API access.
     * Supports both session tokens (with HMAC signature) and admin API keys.
     *
     * @param token The API token to validate
     * @return SecurityMode granted (RESTRICTED, ADMIN, or DESIGNER_ADMIN)
     * @throws SecurityException if token is invalid
     */
    public SecurityMode validateApiToken(String token) throws SecurityException {
        if (token == null || token.trim().isEmpty()) {
            throw new SecurityException("API token required");
        }

        // Check if token is the admin API key (simple string comparison)
        if (isValidAdminKey(token)) {
            LOGGER.debug("Admin API key validated - granting ADMIN mode");
            return SecurityMode.ADMIN;
        }

        // Check if token has signature format (payload.signature)
        if (token.contains(".")) {
            return validateSignedToken(token);
        }

        throw new SecurityException("Invalid API token format");
    }

    /**
     * Validate a signed session token with HMAC verification.
     *
     * @param token The signed token (payload.signature)
     * @return SecurityMode granted by the token
     * @throws SecurityException if token is invalid or expired
     */
    private SecurityMode validateSignedToken(String token) throws SecurityException {
        try {
            // Split token into payload and signature
            String[] parts = token.split("\\.", 2);
            if (parts.length != 2) {
                throw new SecurityException("Invalid token format");
            }

            String payload = parts[0];
            String providedSignature = parts[1];

            // Verify HMAC signature
            Mac hmac = Mac.getInstance("HmacSHA256");
            SecretKeySpec secretKey = new SecretKeySpec(tokenSigningKey, "HmacSHA256");
            hmac.init(secretKey);
            byte[] expectedSignatureBytes = hmac.doFinal(payload.getBytes(StandardCharsets.UTF_8));
            String expectedSignature = Base64.getEncoder().encodeToString(expectedSignatureBytes);

            // Constant-time comparison to prevent timing attacks
            if (!MessageDigest.isEqual(
                    providedSignature.getBytes(StandardCharsets.UTF_8),
                    expectedSignature.getBytes(StandardCharsets.UTF_8))) {
                throw new SecurityException("Invalid token signature");
            }

            // Parse payload to extract security mode
            String[] payloadParts = payload.split("\\|", 3);
            if (payloadParts.length != 3) {
                throw new SecurityException("Invalid token payload");
            }

            long timestamp = Long.parseLong(payloadParts[0]);
            String securityModeStr = payloadParts[1];

            // Check if token exists in active tokens and is not expired
            ApiToken apiToken = activeTokens.get(token);
            if (apiToken == null) {
                throw new SecurityException("Token not found or has been revoked");
            }

            if (apiToken.isExpired()) {
                activeTokens.remove(token);
                throw new SecurityException("Token has expired");
            }

            LOGGER.debug("Signed token validated - granting {} mode", apiToken.securityMode);
            return apiToken.securityMode;

        } catch (NumberFormatException e) {
            throw new SecurityException("Invalid token timestamp");
        } catch (Exception e) {
            LOGGER.error("Token validation failed", e);
            throw new SecurityException("Token validation failed: " + e.getMessage());
        }
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
     * Generate a new session token with HMAC signature.
     * <p>
     * Tokens are cryptographically secure and include:
     * - 128 bits of random data
     * - HMAC-SHA256 signature
     * - Base64 encoding
     *
     * @param securityMode The security mode to grant
     * @param durationSeconds Token lifetime in seconds
     * @return The generated signed token
     */
    public String generateApiToken(SecurityMode securityMode, long durationSeconds) {
        // Generate cryptographically secure random token (16 bytes = 128 bits)
        byte[] randomBytes = new byte[16];
        secureRandom.nextBytes(randomBytes);

        // Create token payload: timestamp|securityMode|randomData
        Instant now = Instant.now();
        Instant expiresAt = now.plusSeconds(durationSeconds);
        long timestamp = now.toEpochMilli();

        String payload = timestamp + "|" + securityMode.name() + "|" + Base64.getEncoder().encodeToString(randomBytes);

        // Generate HMAC signature
        String signature;
        try {
            Mac hmac = Mac.getInstance("HmacSHA256");
            SecretKeySpec secretKey = new SecretKeySpec(tokenSigningKey, "HmacSHA256");
            hmac.init(secretKey);
            byte[] signatureBytes = hmac.doFinal(payload.getBytes(StandardCharsets.UTF_8));
            signature = Base64.getEncoder().encodeToString(signatureBytes);
        } catch (Exception e) {
            LOGGER.error("Failed to generate HMAC signature", e);
            throw new RuntimeException("Token generation failed", e);
        }

        // Final token format: payload.signature (similar to JWT)
        String token = payload + "." + signature;

        // Store token metadata
        ApiToken apiToken = new ApiToken(token, securityMode, now, expiresAt);
        activeTokens.put(token, apiToken);

        LOGGER.info("Generated signed session token with {} mode (expires in {} seconds)", securityMode, durationSeconds);

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
