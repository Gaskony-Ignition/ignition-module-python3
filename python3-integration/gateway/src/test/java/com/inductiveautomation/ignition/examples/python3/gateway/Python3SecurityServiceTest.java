package com.inductiveautomation.ignition.examples.python3.gateway;

import com.inductiveautomation.ignition.gateway.dataroutes.RequestContext;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Integration tests for Python3SecurityService.
 * Tests authentication, authorization, and security mode determination.
 *
 * Phase 3 - Day 11-12: Security integration testing
 */
class Python3SecurityServiceTest {

    private Python3SecurityService securityService;
    private GatewayHook mockGatewayHook;

    @BeforeEach
    void setUp() {
        mockGatewayHook = mock(GatewayHook.class);

        // Clear any existing admin key system property
        System.clearProperty("ignition.python3.admin.apikey");

        // Create service (no admin key configured)
        securityService = new Python3SecurityService(mockGatewayHook);
    }

    // ============================================================
    // Designer Mode Detection Tests
    // ============================================================

    @Test
    void testDetermineSecurityMode_DesignerRequest() {
        // Setup: Mock Designer IDE request
        RequestContext requestContext = createMockRequest("Ignition-Designer/8.3", null);

        // Execute
        SecurityMode mode = securityService.determineSecurityMode(requestContext);

        // Verify: Should grant DESIGNER_ADMIN mode
        assertThat(mode).isEqualTo(SecurityMode.DESIGNER_ADMIN);
    }

    @Test
    void testDetermineSecurityMode_DesignerRequest_WithSource() {
        // Setup: Mock Designer IDE request with X-Source header
        HttpServletRequest httpRequest = mock(HttpServletRequest.class);
        when(httpRequest.getHeader("User-Agent")).thenReturn("Ignition-Designer/8.3");
        when(httpRequest.getHeader("X-Source")).thenReturn("Python3-IDE");

        RequestContext requestContext = mock(RequestContext.class);
        when(requestContext.getRequest()).thenReturn(httpRequest);

        // Execute
        SecurityMode mode = securityService.determineSecurityMode(requestContext);

        // Verify: Should grant DESIGNER_ADMIN mode
        assertThat(mode).isEqualTo(SecurityMode.DESIGNER_ADMIN);
    }

    @Test
    void testDetermineSecurityMode_DesignerRequest_CaseInsensitive() {
        // Setup: Various user agent formats
        String[] userAgents = {
            "Ignition-Designer/8.3",
            "ignition-designer/8.3",
            "IGNITION-DESIGNER/8.3",
            "Mozilla/5.0 Ignition-Designer/8.3"
        };

        for (String userAgent : userAgents) {
            RequestContext requestContext = createMockRequest(userAgent, null);
            SecurityMode mode = securityService.determineSecurityMode(requestContext);
            assertThat(mode)
                .withFailMessage("Failed for User-Agent: " + userAgent)
                .isEqualTo(SecurityMode.DESIGNER_ADMIN);
        }
    }

    // ============================================================
    // Admin API Key Tests
    // ============================================================

    @Test
    void testDetermineSecurityMode_AdminApiKey_Valid() {
        // Setup: Configure admin API key (32+ characters)
        String adminKey = "a".repeat(32); // 32 character key
        System.setProperty("ignition.python3.admin.apikey", adminKey);
        securityService = new Python3SecurityService(mockGatewayHook);

        RequestContext requestContext = createMockRequest(null, "Bearer " + adminKey);

        // Execute
        SecurityMode mode = securityService.determineSecurityMode(requestContext);

        // Verify: Should grant ADMIN mode
        assertThat(mode).isEqualTo(SecurityMode.ADMIN);

        // Cleanup
        System.clearProperty("ignition.python3.admin.apikey");
    }

    @Test
    void testDetermineSecurityMode_AdminApiKey_Invalid() {
        // Setup: Configure admin API key
        String correctKey = "a".repeat(32);
        String wrongKey = "b".repeat(32);
        System.setProperty("ignition.python3.admin.apikey", correctKey);
        securityService = new Python3SecurityService(mockGatewayHook);

        RequestContext requestContext = createMockRequest(null, "Bearer " + wrongKey);

        // Execute
        SecurityMode mode = securityService.determineSecurityMode(requestContext);

        // Verify: Should default to RESTRICTED mode
        assertThat(mode).isEqualTo(SecurityMode.RESTRICTED);

        // Cleanup
        System.clearProperty("ignition.python3.admin.apikey");
    }

    @Test
    void testDetermineSecurityMode_AdminApiKey_TooShort() {
        // Setup: Try to configure too-short admin key (should fail)
        String shortKey = "a".repeat(16); // Only 16 characters
        System.setProperty("ignition.python3.admin.apikey", shortKey);

        // Execute & Verify: Should throw exception
        assertThatThrownBy(() -> new Python3SecurityService(mockGatewayHook))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("at least 32 characters");

        // Cleanup
        System.clearProperty("ignition.python3.admin.apikey");
    }

    @Test
    void testDetermineSecurityMode_LegacyAdminKeyHeader() {
        // Setup: Configure admin API key
        String adminKey = "a".repeat(32);
        System.setProperty("ignition.python3.admin.apikey", adminKey);
        securityService = new Python3SecurityService(mockGatewayHook);

        // Setup: Mock request with legacy X-Python3-Admin-Key header
        HttpServletRequest httpRequest = mock(HttpServletRequest.class);
        when(httpRequest.getHeader("User-Agent")).thenReturn(null);
        when(httpRequest.getHeader("Authorization")).thenReturn(null);
        when(httpRequest.getHeader("X-Python3-Admin-Key")).thenReturn(adminKey);

        RequestContext requestContext = mock(RequestContext.class);
        when(requestContext.getRequest()).thenReturn(httpRequest);

        // Execute
        SecurityMode mode = securityService.determineSecurityMode(requestContext);

        // Verify: Should grant ADMIN mode
        assertThat(mode).isEqualTo(SecurityMode.ADMIN);

        // Cleanup
        System.clearProperty("ignition.python3.admin.apikey");
    }

    // ============================================================
    // Unauthenticated/RESTRICTED Mode Tests
    // ============================================================

    @Test
    void testDetermineSecurityMode_NoAuthentication() {
        // Setup: Mock request with no authentication headers
        RequestContext requestContext = createMockRequest(null, null);

        // Execute
        SecurityMode mode = securityService.determineSecurityMode(requestContext);

        // Verify: Should default to RESTRICTED mode
        assertThat(mode).isEqualTo(SecurityMode.RESTRICTED);
    }

    @Test
    void testDetermineSecurityMode_EmptyHeaders() {
        // Setup: Mock request with empty headers
        HttpServletRequest httpRequest = mock(HttpServletRequest.class);
        when(httpRequest.getHeader("User-Agent")).thenReturn("");
        when(httpRequest.getHeader("Authorization")).thenReturn("");

        RequestContext requestContext = mock(RequestContext.class);
        when(requestContext.getRequest()).thenReturn(httpRequest);

        // Execute
        SecurityMode mode = securityService.determineSecurityMode(requestContext);

        // Verify: Should default to RESTRICTED mode
        assertThat(mode).isEqualTo(SecurityMode.RESTRICTED);
    }

    // ============================================================
    // API Token Tests
    // ============================================================

    @Test
    void testGenerateApiToken_AdminMode() {
        // Execute: Generate admin token
        String token = securityService.generateApiToken(SecurityMode.ADMIN, 3600);

        // Verify: Token should be valid UUID
        assertThat(token).isNotEmpty();
        assertThat(token).hasSize(36); // UUID format

        // Verify: Token should validate
        SecurityMode mode = securityService.validateApiToken(token);
        assertThat(mode).isEqualTo(SecurityMode.ADMIN);
    }

    @Test
    void testGenerateApiToken_RestrictedMode() {
        // Execute: Generate restricted token
        String token = securityService.generateApiToken(SecurityMode.RESTRICTED, 3600);

        // Verify: Token should validate with RESTRICTED mode
        SecurityMode mode = securityService.validateApiToken(token);
        assertThat(mode).isEqualTo(SecurityMode.RESTRICTED);
    }

    @Test
    void testRevokeApiToken() {
        // Setup: Generate token
        String token = securityService.generateApiToken(SecurityMode.ADMIN, 3600);

        // Verify: Token is valid
        assertThatNoException().isThrownBy(() -> securityService.validateApiToken(token));

        // Execute: Revoke token
        boolean revoked = securityService.revokeApiToken(token);

        // Verify: Token revocation succeeded
        assertThat(revoked).isTrue();

        // Verify: Token is now invalid
        assertThatThrownBy(() -> securityService.validateApiToken(token))
            .isInstanceOf(SecurityException.class)
            .hasMessageContaining("Invalid API token");
    }

    @Test
    void testRevokeApiToken_NotFound() {
        // Execute: Try to revoke non-existent token
        boolean revoked = securityService.revokeApiToken("non-existent-token");

        // Verify: Should return false
        assertThat(revoked).isFalse();
    }

    @Test
    void testValidateApiToken_ExpiredToken() throws InterruptedException {
        // Setup: Generate token with 1 second expiration
        String token = securityService.generateApiToken(SecurityMode.ADMIN, 1);

        // Wait for token to expire
        Thread.sleep(1100);

        // Execute & Verify: Should throw exception
        assertThatThrownBy(() -> securityService.validateApiToken(token))
            .isInstanceOf(SecurityException.class)
            .hasMessageContaining("expired");
    }

    @Test
    void testValidateApiToken_NullToken() {
        // Execute & Verify: Should throw exception
        assertThatThrownBy(() -> securityService.validateApiToken(null))
            .isInstanceOf(SecurityException.class)
            .hasMessageContaining("required");
    }

    @Test
    void testValidateApiToken_EmptyToken() {
        // Execute & Verify: Should throw exception
        assertThatThrownBy(() -> securityService.validateApiToken(""))
            .isInstanceOf(SecurityException.class)
            .hasMessageContaining("required");
    }

    // ============================================================
    // HTTPS Enforcement Tests
    // ============================================================

    @Test
    void testEnforceHttpsRequirement_AdminMode_Https() {
        // Setup: Mock HTTPS request
        HttpServletRequest httpRequest = mock(HttpServletRequest.class);
        when(httpRequest.isSecure()).thenReturn(true);

        RequestContext requestContext = mock(RequestContext.class);
        when(requestContext.getRequest()).thenReturn(httpRequest);

        // Execute & Verify: Should not throw exception
        assertThatNoException().isThrownBy(() ->
            securityService.enforceHttpsRequirement(SecurityMode.ADMIN, requestContext)
        );
    }

    @Test
    void testEnforceHttpsRequirement_AdminMode_Http() {
        // Setup: Mock HTTP request (not secure)
        HttpServletRequest httpRequest = mock(HttpServletRequest.class);
        when(httpRequest.isSecure()).thenReturn(false);

        RequestContext requestContext = mock(RequestContext.class);
        when(requestContext.getRequest()).thenReturn(httpRequest);

        // Execute & Verify: Should throw exception
        assertThatThrownBy(() ->
            securityService.enforceHttpsRequirement(SecurityMode.ADMIN, requestContext)
        )
            .isInstanceOf(SecurityException.class)
            .hasMessageContaining("HTTPS");
    }

    @Test
    void testEnforceHttpsRequirement_AdminMode_HttpsDisabled() {
        // Setup: Disable HTTPS requirement
        System.setProperty("ignition.python3.admin.requirehttps", "false");

        HttpServletRequest httpRequest = mock(HttpServletRequest.class);
        when(httpRequest.isSecure()).thenReturn(false);

        RequestContext requestContext = mock(RequestContext.class);
        when(requestContext.getRequest()).thenReturn(httpRequest);

        // Execute & Verify: Should not throw exception (HTTPS disabled)
        assertThatNoException().isThrownBy(() ->
            securityService.enforceHttpsRequirement(SecurityMode.ADMIN, requestContext)
        );

        // Cleanup
        System.clearProperty("ignition.python3.admin.requirehttps");
    }

    @Test
    void testEnforceHttpsRequirement_DesignerAdminMode_Http() {
        // Setup: Mock HTTP request
        HttpServletRequest httpRequest = mock(HttpServletRequest.class);
        when(httpRequest.isSecure()).thenReturn(false);

        RequestContext requestContext = mock(RequestContext.class);
        when(requestContext.getRequest()).thenReturn(httpRequest);

        // Execute & Verify: DESIGNER_ADMIN mode does not require HTTPS
        assertThatNoException().isThrownBy(() ->
            securityService.enforceHttpsRequirement(SecurityMode.DESIGNER_ADMIN, requestContext)
        );
    }

    @Test
    void testEnforceHttpsRequirement_RestrictedMode_Http() {
        // Setup: Mock HTTP request
        HttpServletRequest httpRequest = mock(HttpServletRequest.class);
        when(httpRequest.isSecure()).thenReturn(false);

        RequestContext requestContext = mock(RequestContext.class);
        when(requestContext.getRequest()).thenReturn(httpRequest);

        // Execute & Verify: RESTRICTED mode does not require HTTPS
        assertThatNoException().isThrownBy(() ->
            securityService.enforceHttpsRequirement(SecurityMode.RESTRICTED, requestContext)
        );
    }

    // ============================================================
    // Priority Order Tests
    // ============================================================

    @Test
    void testDetermineSecurityMode_DesignerTakesPriority() {
        // Setup: Configure admin key AND set Designer user-agent
        String adminKey = "a".repeat(32);
        System.setProperty("ignition.python3.admin.apikey", adminKey);
        securityService = new Python3SecurityService(mockGatewayHook);

        // Mock request with BOTH Designer user-agent AND admin key
        HttpServletRequest httpRequest = mock(HttpServletRequest.class);
        when(httpRequest.getHeader("User-Agent")).thenReturn("Ignition-Designer/8.3");
        when(httpRequest.getHeader("Authorization")).thenReturn("Bearer " + adminKey);

        RequestContext requestContext = mock(RequestContext.class);
        when(requestContext.getRequest()).thenReturn(httpRequest);

        // Execute
        SecurityMode mode = securityService.determineSecurityMode(requestContext);

        // Verify: Designer should take priority over admin key
        assertThat(mode).isEqualTo(SecurityMode.DESIGNER_ADMIN);

        // Cleanup
        System.clearProperty("ignition.python3.admin.apikey");
    }

    // ============================================================
    // Metrics Tests
    // ============================================================

    @Test
    void testGetActiveTokenCount() {
        // Setup: Generate multiple tokens
        String token1 = securityService.generateApiToken(SecurityMode.ADMIN, 3600);
        String token2 = securityService.generateApiToken(SecurityMode.RESTRICTED, 3600);
        String token3 = securityService.generateApiToken(SecurityMode.ADMIN, 3600);

        // Execute
        int count = securityService.getActiveTokenCount();

        // Verify: Should have 3 active tokens
        assertThat(count).isEqualTo(3);

        // Revoke one
        securityService.revokeApiToken(token1);

        // Verify: Should have 2 active tokens
        assertThat(securityService.getActiveTokenCount()).isEqualTo(2);
    }

    @Test
    void testGetActiveTokenCount_CleansExpiredTokens() throws InterruptedException {
        // Setup: Generate short-lived token
        securityService.generateApiToken(SecurityMode.ADMIN, 1);

        // Verify: Initially 1 token
        assertThat(securityService.getActiveTokenCount()).isEqualTo(1);

        // Wait for expiration
        Thread.sleep(1100);

        // Execute: Get count (should clean up expired tokens)
        int count = securityService.getActiveTokenCount();

        // Verify: Should have 0 active tokens (expired one was cleaned)
        assertThat(count).isEqualTo(0);
    }

    // ============================================================
    // Helper Methods
    // ============================================================

    /**
     * Create a mock RequestContext with specified headers
     */
    private RequestContext createMockRequest(String userAgent, String authorization) {
        HttpServletRequest httpRequest = mock(HttpServletRequest.class);
        when(httpRequest.getHeader("User-Agent")).thenReturn(userAgent);
        when(httpRequest.getHeader("Authorization")).thenReturn(authorization);

        RequestContext requestContext = mock(RequestContext.class);
        when(requestContext.getRequest()).thenReturn(httpRequest);

        return requestContext;
    }
}
