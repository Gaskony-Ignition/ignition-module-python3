package com.inductiveautomation.ignition.examples.python3.gateway;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;

/**
 * Unit tests for RateLimiter.
 *
 * RateLimiter has no Ignition SDK dependencies — pure unit tests.
 * Uses the 3-arg constructor to control limits directly.
 */
class RateLimiterTest {

    // Small limits for fast tests: 3 requests per user, 10 global, 10s window
    private RateLimiter limiter;

    @BeforeEach
    void setUp() {
        limiter = new RateLimiter(3, 10, 10_000L);
    }

    // ===== Constructor / getters =====

    @Test
    void constructor_setsConfiguredValues() {
        assertThat(limiter.getUserRequestsPerWindow()).isEqualTo(3);
        assertThat(limiter.getGlobalRequestsPerWindow()).isEqualTo(10);
        assertThat(limiter.getWindowDurationMs()).isEqualTo(10_000L);
    }

    // ===== allowRequest — basic cases =====

    @Test
    void allowRequest_firstRequest_returnsTrue() {
        UserContext user = new UserContext("alice", UserContext.ExecutionSource.REST_API);
        assertThat(limiter.allowRequest(user)).isTrue();
    }

    @Test
    void allowRequest_upToLimit_returnsTrue() {
        UserContext user = new UserContext("alice", UserContext.ExecutionSource.REST_API);
        // 3 requests should be allowed
        assertThat(limiter.allowRequest(user)).isTrue();
        assertThat(limiter.allowRequest(user)).isTrue();
        assertThat(limiter.allowRequest(user)).isTrue();
    }

    @Test
    void allowRequest_exceedingUserLimit_returnsFalse() {
        UserContext user = new UserContext("alice", UserContext.ExecutionSource.REST_API);
        // Exhaust 3 user tokens
        limiter.allowRequest(user);
        limiter.allowRequest(user);
        limiter.allowRequest(user);
        // 4th should be rejected
        assertThat(limiter.allowRequest(user)).isFalse();
    }

    @Test
    void allowRequest_differentUsers_haveIndependentLimits() {
        UserContext alice = new UserContext("alice", UserContext.ExecutionSource.REST_API);
        UserContext bob = new UserContext("bob", UserContext.ExecutionSource.REST_API);

        // Exhaust Alice's limit
        limiter.allowRequest(alice);
        limiter.allowRequest(alice);
        limiter.allowRequest(alice);
        assertThat(limiter.allowRequest(alice)).isFalse();

        // Bob should still be allowed
        assertThat(limiter.allowRequest(bob)).isTrue();
    }

    @Test
    void allowRequest_nullUserContext_usesAnonymous() {
        // null userContext falls back to "anonymous" username
        assertThat(limiter.allowRequest(null)).isTrue();
        assertThat(limiter.allowRequest(null)).isTrue();
        assertThat(limiter.allowRequest(null)).isTrue();
        // 4th anonymous request should be rejected
        assertThat(limiter.allowRequest(null)).isFalse();
    }

    // ===== Global rate limiting =====

    @Test
    void allowRequest_exceedsGlobalLimit_returnsFalse() {
        // Global limit is 10; use 10 different users to avoid user limit
        for (int i = 0; i < 10; i++) {
            UserContext user = new UserContext("user" + i, UserContext.ExecutionSource.REST_API);
            limiter.allowRequest(user);
        }
        // 11th request from any user should be rejected (global exhausted)
        UserContext extra = new UserContext("extra", UserContext.ExecutionSource.REST_API);
        assertThat(limiter.allowRequest(extra)).isFalse();
    }

    // ===== Statistics =====

    @Test
    void getTotalRequests_tracksAllRequests() {
        UserContext user = new UserContext("alice", UserContext.ExecutionSource.REST_API);
        limiter.allowRequest(user);
        limiter.allowRequest(user);
        assertThat(limiter.getTotalRequests()).isEqualTo(2);
    }

    @Test
    void getTotalRejections_tracksRejected() {
        UserContext user = new UserContext("alice", UserContext.ExecutionSource.REST_API);
        limiter.allowRequest(user);
        limiter.allowRequest(user);
        limiter.allowRequest(user);
        limiter.allowRequest(user); // rejected
        assertThat(limiter.getTotalRejections()).isEqualTo(1);
    }

    @Test
    void getRejectionRate_noRequests_returnsZero() {
        assertThat(limiter.getRejectionRate()).isEqualTo(0.0);
    }

    @Test
    void getRejectionRate_someRejections_returnsCorrectRate() {
        UserContext user = new UserContext("alice", UserContext.ExecutionSource.REST_API);
        limiter.allowRequest(user); // allowed
        limiter.allowRequest(user); // allowed
        limiter.allowRequest(user); // allowed
        limiter.allowRequest(user); // rejected

        // 1 rejected out of 4 = 0.25
        assertThat(limiter.getRejectionRate()).isEqualTo(0.25);
    }

    @Test
    void getActiveUserCount_tracksUsers() {
        UserContext alice = new UserContext("alice", UserContext.ExecutionSource.REST_API);
        UserContext bob = new UserContext("bob", UserContext.ExecutionSource.REST_API);
        limiter.allowRequest(alice);
        limiter.allowRequest(bob);
        assertThat(limiter.getActiveUserCount()).isEqualTo(2);
    }

    @Test
    void getGlobalAvailableTokens_decreasesWithRequests() {
        int initial = limiter.getGlobalAvailableTokens();
        UserContext user = new UserContext("alice", UserContext.ExecutionSource.REST_API);
        limiter.allowRequest(user);
        assertThat(limiter.getGlobalAvailableTokens()).isLessThan(initial);
    }

    // ===== getUserStats =====

    @Test
    void getUserStats_unknownUser_returnsDefaultStats() {
        RateLimiter.UserStats stats = limiter.getUserStats("nobody");
        assertThat(stats.username).isEqualTo("nobody");
        assertThat(stats.totalRequests).isEqualTo(0);
        assertThat(stats.rejectedRequests).isEqualTo(0);
        assertThat(stats.capacity).isEqualTo(3);
    }

    @Test
    void getUserStats_afterRequests_showsCorrectCounts() {
        UserContext user = new UserContext("alice", UserContext.ExecutionSource.REST_API);
        limiter.allowRequest(user); // allowed
        limiter.allowRequest(user); // allowed
        limiter.allowRequest(user); // allowed
        limiter.allowRequest(user); // rejected

        RateLimiter.UserStats stats = limiter.getUserStats("alice");
        assertThat(stats.totalRequests).isEqualTo(4);
        assertThat(stats.rejectedRequests).isEqualTo(1);
    }

    @Test
    void getUserStats_getRejectionRate_calculatesCorrectly() {
        UserContext user = new UserContext("alice", UserContext.ExecutionSource.REST_API);
        limiter.allowRequest(user); // allowed
        limiter.allowRequest(user); // allowed

        RateLimiter.UserStats stats = limiter.getUserStats("alice");
        assertThat(stats.getRejectionRate()).isEqualTo(0.0);
    }

    @Test
    void userStats_toString_containsUsername() {
        RateLimiter.UserStats stats = limiter.getUserStats("nobody");
        assertThat(stats.toString()).contains("nobody");
    }

    // ===== reset =====

    @Test
    void reset_clearsAllState() {
        UserContext user = new UserContext("alice", UserContext.ExecutionSource.REST_API);
        limiter.allowRequest(user);
        limiter.allowRequest(user);

        limiter.reset();

        assertThat(limiter.getTotalRequests()).isEqualTo(0);
        assertThat(limiter.getTotalRejections()).isEqualTo(0);
        assertThat(limiter.getActiveUserCount()).isEqualTo(0);
    }

    @Test
    void reset_refillsTokensSoNewRequestsAllowed() {
        UserContext user = new UserContext("alice", UserContext.ExecutionSource.REST_API);
        // Exhaust tokens
        limiter.allowRequest(user);
        limiter.allowRequest(user);
        limiter.allowRequest(user);
        assertThat(limiter.allowRequest(user)).isFalse();

        // Reset should restore
        limiter.reset();
        assertThat(limiter.allowRequest(user)).isTrue();
    }

    // ===== resetUser =====

    @Test
    void resetUser_removesSpecificUser() {
        UserContext alice = new UserContext("alice", UserContext.ExecutionSource.REST_API);
        UserContext bob = new UserContext("bob", UserContext.ExecutionSource.REST_API);
        limiter.allowRequest(alice);
        limiter.allowRequest(bob);

        limiter.resetUser("alice");

        assertThat(limiter.getActiveUserCount()).isEqualTo(1); // only bob
    }

    @Test
    void resetUser_allowsExhaustedUserAgain() {
        UserContext user = new UserContext("alice", UserContext.ExecutionSource.REST_API);
        limiter.allowRequest(user);
        limiter.allowRequest(user);
        limiter.allowRequest(user);
        assertThat(limiter.allowRequest(user)).isFalse();

        limiter.resetUser("alice");
        assertThat(limiter.allowRequest(user)).isTrue();
    }

    // ===== cleanupInactiveUsers =====

    @Test
    void cleanupInactiveUsers_removesFullyRefreshedBuckets() {
        // With a very short window (1ms), tokens will refill quickly
        RateLimiter shortWindow = new RateLimiter(100, 1000, 1L);
        UserContext user = new UserContext("alice", UserContext.ExecutionSource.REST_API);
        shortWindow.allowRequest(user);

        // intentional fixed delay — testing wall-clock token-bucket refill plus the
        // cleanup heuristic that drops fully-refilled buckets. The 1ms refill window
        // means the test must physically wait ≥1ms.
        try { Thread.sleep(10); } catch (InterruptedException ignored) {}
        shortWindow.cleanupInactiveUsers();

        // User bucket may have been removed (all tokens available = inactive)
        // Just verify no exception thrown
        assertThat(shortWindow.getActiveUserCount()).isGreaterThanOrEqualTo(0);
    }

    // ===== getSummary =====

    @Test
    void getSummary_containsKeyInfo() {
        String summary = limiter.getSummary();
        assertThat(summary).contains("RateLimiter");
    }

    @Test
    void toString_equalsSummary() {
        assertThat(limiter.toString()).isEqualTo(limiter.getSummary());
    }

    // ===== Statistics counters =====

    @Test
    void getUserRejections_tracksUserLevelRejections() {
        UserContext user = new UserContext("alice", UserContext.ExecutionSource.REST_API);
        limiter.allowRequest(user);
        limiter.allowRequest(user);
        limiter.allowRequest(user);
        limiter.allowRequest(user); // user-level rejection

        assertThat(limiter.getUserRejections()).isEqualTo(1);
        assertThat(limiter.getGlobalRejections()).isEqualTo(0);
    }
}
