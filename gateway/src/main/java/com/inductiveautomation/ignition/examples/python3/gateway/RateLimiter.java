package com.inductiveautomation.ignition.examples.python3.gateway;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Rate limiter for Python execution requests.
 *
 * Implements token bucket algorithm for rate limiting:
 * - Per-user rate limits (configurable)
 * - Global rate limits (prevents system overload)
 * - Configurable window duration
 * - Automatic token refill
 *
 * Configuration via system properties:
 * - ignition.python3.ratelimit.user.requests (default: 60 req/min)
 * - ignition.python3.ratelimit.global.requests (default: 300 req/min)
 * - ignition.python3.ratelimit.window.ms (default: 60000ms = 1 minute)
 *
 * @since v2.14.0 (Phase 2 Week 5-6: Security Enhancements)
 */
public class RateLimiter {

    private static final Logger logger = LoggerFactory.getLogger(RateLimiter.class);

    // Default limits
    private static final int DEFAULT_USER_REQUESTS_PER_WINDOW = 60;        // 60 req/min per user
    private static final int DEFAULT_GLOBAL_REQUESTS_PER_WINDOW = 300;     // 300 req/min total
    private static final long DEFAULT_WINDOW_DURATION_MS = 60000;          // 1 minute

    // Configuration
    private final int userRequestsPerWindow;
    private final int globalRequestsPerWindow;
    private final long windowDurationMs;

    // Per-user rate limiting
    private final ConcurrentHashMap<String, UserBucket> userBuckets;

    // Global rate limiting
    private final TokenBucket globalBucket;

    // Statistics
    private final AtomicLong totalRequests;
    private final AtomicLong totalRejections;
    private final AtomicLong userRejections;
    private final AtomicLong globalRejections;

    /**
     * Token bucket for rate limiting.
     */
    private static class TokenBucket {
        private final int capacity;
        private final long refillIntervalMs;
        private final AtomicInteger tokens;
        private final AtomicLong lastRefillTime;

        TokenBucket(int capacity, long refillIntervalMs) {
            this.capacity = capacity;
            this.refillIntervalMs = refillIntervalMs;
            this.tokens = new AtomicInteger(capacity);
            this.lastRefillTime = new AtomicLong(System.currentTimeMillis());
        }

        /**
         * Try to consume a token.
         * @return true if token consumed, false if rate limit exceeded
         */
        boolean tryConsume() {
            refillTokens();

            while (true) {
                int current = tokens.get();
                if (current <= 0) {
                    return false; // Rate limit exceeded
                }

                if (tokens.compareAndSet(current, current - 1)) {
                    return true; // Token consumed
                }
            }
        }

        /**
         * Refill tokens based on time elapsed.
         */
        private void refillTokens() {
            long now = System.currentTimeMillis();
            long lastRefill = lastRefillTime.get();
            long timeSinceRefill = now - lastRefill;

            if (timeSinceRefill >= refillIntervalMs) {
                // Refill to capacity
                if (lastRefillTime.compareAndSet(lastRefill, now)) {
                    tokens.set(capacity);
                }
            }
        }

        int getAvailableTokens() {
            refillTokens();
            return Math.max(0, tokens.get());
        }

        int getCapacity() {
            return capacity;
        }
    }

    /**
     * Per-user bucket.
     */
    private static class UserBucket extends TokenBucket {
        private final String username;
        private final AtomicLong totalRequests;
        private final AtomicLong rejectedRequests;

        UserBucket(String username, int capacity, long refillIntervalMs) {
            super(capacity, refillIntervalMs);
            this.username = username;
            this.totalRequests = new AtomicLong(0);
            this.rejectedRequests = new AtomicLong(0);
        }

        void recordRequest(boolean allowed) {
            totalRequests.incrementAndGet();
            if (!allowed) {
                rejectedRequests.incrementAndGet();
            }
        }

        long getTotalRequests() {
            return totalRequests.get();
        }

        long getRejectedRequests() {
            return rejectedRequests.get();
        }
    }

    /**
     * Create rate limiter with default settings.
     */
    public RateLimiter() {
        this(
            getIntProperty("ignition.python3.ratelimit.user.requests", DEFAULT_USER_REQUESTS_PER_WINDOW),
            getIntProperty("ignition.python3.ratelimit.global.requests", DEFAULT_GLOBAL_REQUESTS_PER_WINDOW),
            getLongProperty("ignition.python3.ratelimit.window.ms", DEFAULT_WINDOW_DURATION_MS)
        );
    }

    /**
     * Create rate limiter with custom settings.
     *
     * @param userRequestsPerWindow Max requests per user per window
     * @param globalRequestsPerWindow Max requests globally per window
     * @param windowDurationMs Window duration in milliseconds
     */
    public RateLimiter(int userRequestsPerWindow, int globalRequestsPerWindow, long windowDurationMs) {
        this.userRequestsPerWindow = userRequestsPerWindow;
        this.globalRequestsPerWindow = globalRequestsPerWindow;
        this.windowDurationMs = windowDurationMs;

        this.userBuckets = new ConcurrentHashMap<>();
        this.globalBucket = new TokenBucket(globalRequestsPerWindow, windowDurationMs);

        this.totalRequests = new AtomicLong(0);
        this.totalRejections = new AtomicLong(0);
        this.userRejections = new AtomicLong(0);
        this.globalRejections = new AtomicLong(0);

        logger.info("RateLimiter initialized: userLimit={} req/{}ms, globalLimit={} req/{}ms",
            userRequestsPerWindow, windowDurationMs, globalRequestsPerWindow, windowDurationMs);
    }

    /**
     * Check if request is allowed for user.
     *
     * @param userContext User making the request
     * @return true if allowed, false if rate limit exceeded
     */
    public boolean allowRequest(UserContext userContext) {
        totalRequests.incrementAndGet();

        // Check global rate limit first
        if (!globalBucket.tryConsume()) {
            globalRejections.incrementAndGet();
            totalRejections.incrementAndGet();
            logger.warn("Global rate limit exceeded: {} req/{}ms", globalRequestsPerWindow, windowDurationMs);
            return false;
        }

        // Check per-user rate limit
        String username = userContext != null ? userContext.getUsername() : "anonymous";
        UserBucket userBucket = getUserBucket(username);

        boolean allowed = userBucket.tryConsume();
        userBucket.recordRequest(allowed);

        if (!allowed) {
            userRejections.incrementAndGet();
            totalRejections.incrementAndGet();
            logger.warn("User '{}' rate limit exceeded: {} req/{}ms (source: {})",
                username, userRequestsPerWindow, windowDurationMs,
                userContext != null ? userContext.getSource() : "UNKNOWN");
        }

        return allowed;
    }

    /**
     * Get or create user bucket.
     */
    private UserBucket getUserBucket(String username) {
        return userBuckets.computeIfAbsent(username,
            key -> new UserBucket(key, userRequestsPerWindow, windowDurationMs));
    }

    /**
     * Get user statistics.
     */
    public UserStats getUserStats(String username) {
        UserBucket bucket = userBuckets.get(username);
        if (bucket == null) {
            return new UserStats(username, 0, 0, userRequestsPerWindow, userRequestsPerWindow);
        }

        return new UserStats(
            username,
            bucket.getTotalRequests(),
            bucket.getRejectedRequests(),
            bucket.getAvailableTokens(),
            bucket.getCapacity()
        );
    }

    /**
     * User statistics snapshot.
     */
    public static class UserStats {
        public final String username;
        public final long totalRequests;
        public final long rejectedRequests;
        public final int availableTokens;
        public final int capacity;

        UserStats(String username, long totalRequests, long rejectedRequests,
                 int availableTokens, int capacity) {
            this.username = username;
            this.totalRequests = totalRequests;
            this.rejectedRequests = rejectedRequests;
            this.availableTokens = availableTokens;
            this.capacity = capacity;
        }

        public double getRejectionRate() {
            if (totalRequests == 0) return 0.0;
            return (double) rejectedRequests / totalRequests;
        }

        @Override
        public String toString() {
            return String.format(
                "UserStats{user=%s, total=%d, rejected=%d (%.1f%%), tokens=%d/%d}",
                username, totalRequests, rejectedRequests, getRejectionRate() * 100,
                availableTokens, capacity
            );
        }
    }

    /**
     * Reset rate limits for all users (for testing or manual override).
     */
    public void reset() {
        userBuckets.clear();
        totalRequests.set(0);
        totalRejections.set(0);
        userRejections.set(0);
        globalRejections.set(0);
        logger.info("Rate limiter reset");
    }

    /**
     * Reset rate limit for specific user.
     */
    public void resetUser(String username) {
        userBuckets.remove(username);
        logger.info("Rate limit reset for user: {}", username);
    }

    /**
     * Clean up inactive user buckets (optional, for long-running systems).
     */
    public void cleanupInactiveUsers() {
        // Remove users with no recent activity (all tokens available = no recent requests)
        int removed = 0;
        for (java.util.Map.Entry<String, UserBucket> entry : userBuckets.entrySet()) {
            UserBucket bucket = entry.getValue();
            if (bucket.getAvailableTokens() == bucket.getCapacity()) {
                userBuckets.remove(entry.getKey());
                removed++;
            }
        }
        if (removed > 0) {
            logger.debug("Cleaned up {} inactive user buckets", removed);
        }
    }

    // Getters and statistics

    public int getUserRequestsPerWindow() {
        return userRequestsPerWindow;
    }

    public int getGlobalRequestsPerWindow() {
        return globalRequestsPerWindow;
    }

    public long getWindowDurationMs() {
        return windowDurationMs;
    }

    public long getTotalRequests() {
        return totalRequests.get();
    }

    public long getTotalRejections() {
        return totalRejections.get();
    }

    public long getUserRejections() {
        return userRejections.get();
    }

    public long getGlobalRejections() {
        return globalRejections.get();
    }

    public double getRejectionRate() {
        long total = totalRequests.get();
        if (total == 0) return 0.0;
        return (double) totalRejections.get() / total;
    }

    public int getActiveUserCount() {
        return userBuckets.size();
    }

    public int getGlobalAvailableTokens() {
        return globalBucket.getAvailableTokens();
    }

    /**
     * Get summary statistics.
     */
    public String getSummary() {
        return String.format(
            "RateLimiter{total=%d, rejected=%d (%.1f%%), users=%d, globalTokens=%d/%d, " +
            "userLimit=%d/%dms, globalLimit=%d/%dms}",
            totalRequests.get(),
            totalRejections.get(),
            getRejectionRate() * 100,
            userBuckets.size(),
            globalBucket.getAvailableTokens(),
            globalBucket.getCapacity(),
            userRequestsPerWindow,
            windowDurationMs,
            globalRequestsPerWindow,
            windowDurationMs
        );
    }

    // Helper methods for loading from system properties

    private static int getIntProperty(String key, int defaultValue) {
        String value = System.getProperty(key);
        if (value != null) {
            try {
                return Integer.parseInt(value);
            } catch (NumberFormatException e) {
                logger.warn("Invalid int property {}: {}, using default: {}", key, value, defaultValue);
            }
        }
        return defaultValue;
    }

    private static long getLongProperty(String key, long defaultValue) {
        String value = System.getProperty(key);
        if (value != null) {
            try {
                return Long.parseLong(value);
            } catch (NumberFormatException e) {
                logger.warn("Invalid long property {}: {}, using default: {}", key, value, defaultValue);
            }
        }
        return defaultValue;
    }

    @Override
    public String toString() {
        return getSummary();
    }
}
