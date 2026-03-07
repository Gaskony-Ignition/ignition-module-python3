package com.inductiveautomation.ignition.examples.python3.gateway;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Result Cache - Caches Python execution results for identical code+variables.
 *
 * <p>Uses LRU (Least Recently Used) eviction policy with configurable TTL and max size.
 * Thread-safe implementation using ConcurrentHashMap and synchronized access.
 *
 * <h2>Cache Key Generation:</h2>
 * <ul>
 *   <li>SHA-256 hash of code + serialized variables</li>
 *   <li>Collision-resistant (2^256 possible keys)</li>
 *   <li>Deterministic (same input = same key)</li>
 * </ul>
 *
 * <h2>Eviction Policies:</h2>
 * <ul>
 *   <li><b>LRU:</b> Evict least recently used entries when cache is full</li>
 *   <li><b>TTL:</b> Evict entries older than configured time-to-live</li>
 * </ul>
 *
 * <h2>Usage:</h2>
 * <pre>{@code
 * ResultCache cache = new ResultCache(1000, 300000); // 1000 entries, 5 min TTL
 *
 * // Try to get cached result
 * Python3Result cached = cache.get(code, variables);
 * if (cached != null) {
 *     return cached; // Cache hit!
 * }
 *
 * // Execute and cache result
 * Python3Result result = executor.execute(code, variables);
 * cache.put(code, variables, result);
 * }</pre>
 *
 * @since v2.16.0 (Phase 3 Week 3-4: Performance Optimization)
 */
public class ResultCache {

    private static final Logger logger = LoggerFactory.getLogger(ResultCache.class);

    // Cache configuration
    private final int maxSize;
    private final long ttlMillis;

    // Cache storage (LRU-ordered map)
    private final Map<String, CacheEntry> cache;

    // Cache statistics
    private final AtomicLong hits = new AtomicLong(0);
    private final AtomicLong misses = new AtomicLong(0);
    private final AtomicLong evictions = new AtomicLong(0);

    /**
     * Create a result cache with specified max size and TTL.
     *
     * @param maxSize   Maximum number of cached entries
     * @param ttlMillis Time-to-live in milliseconds (0 = no expiration)
     */
    public ResultCache(int maxSize, long ttlMillis) {
        if (maxSize <= 0) {
            throw new IllegalArgumentException("Max size must be > 0");
        }
        if (ttlMillis < 0) {
            throw new IllegalArgumentException("TTL must be >= 0");
        }

        this.maxSize = maxSize;
        this.ttlMillis = ttlMillis;

        // Create LRU cache using LinkedHashMap with access-order
        // Wrap in synchronizedMap for thread safety
        this.cache = Collections.synchronizedMap(new LinkedHashMap<String, CacheEntry>(maxSize, 0.75f, true) {
            @Override
            protected boolean removeEldestEntry(Map.Entry<String, CacheEntry> eldest) {
                boolean shouldRemove = size() > maxSize;
                if (shouldRemove) {
                    evictions.incrementAndGet();
                }
                return shouldRemove;
            }
        });

        logger.info("ResultCache created: maxSize={}, ttlMillis={}ms", maxSize, ttlMillis);
    }

    /**
     * Get cached result for code+variables combination.
     *
     * @param code      Python code
     * @param variables Variables map
     * @return Cached result, or null if not found or expired
     */
    public Python3Result get(String code, Map<String, Object> variables) {
        String key = generateKey(code, variables);
        CacheEntry entry = cache.get(key);

        if (entry == null) {
            misses.incrementAndGet();
            logger.debug("Cache miss: key={}", key.substring(0, Math.min(16, key.length())));
            return null;
        }

        // Check if expired
        if (isExpired(entry)) {
            cache.remove(key);
            misses.incrementAndGet();
            evictions.incrementAndGet();
            logger.debug("Cache entry expired: key={}", key.substring(0, Math.min(16, key.length())));
            return null;
        }

        // Cache hit
        hits.incrementAndGet();
        logger.debug("Cache hit: key={}", key.substring(0, Math.min(16, key.length())));
        return entry.result;
    }

    /**
     * Put result in cache for code+variables combination.
     *
     * @param code      Python code
     * @param variables Variables map
     * @param result    Execution result
     */
    public void put(String code, Map<String, Object> variables, Python3Result result) {
        if (result == null) {
            return; // Don't cache null results
        }

        String key = generateKey(code, variables);

        // LinkedHashMap will automatically evict eldest entry when size > maxSize
        CacheEntry entry = new CacheEntry(result);
        cache.put(key, entry);

        logger.debug("Cache put: key={}, size={}/{}",
                key.substring(0, Math.min(16, key.length())), cache.size(), maxSize);
    }

    /**
     * Clear all cached entries.
     */
    public synchronized void clear() {
        int size = cache.size();
        cache.clear();
        logger.info("Cache cleared: {} entries removed", size);
    }

    /**
     * Get cache statistics.
     *
     * @return Map of statistics
     */
    public Map<String, Object> getStats() {
        long totalRequests = hits.get() + misses.get();
        double hitRate = totalRequests > 0 ? (hits.get() * 100.0 / totalRequests) : 0.0;

        Map<String, Object> stats = new LinkedHashMap<>();
        stats.put("size", cache.size());
        stats.put("maxSize", maxSize);
        stats.put("hits", hits.get());
        stats.put("misses", misses.get());
        stats.put("evictions", evictions.get());
        stats.put("hitRate", hitRate);
        stats.put("ttlMillis", ttlMillis);

        return stats;
    }

    /**
     * Get cache hit rate percentage.
     *
     * @return Hit rate (0-100%)
     */
    public double getHitRate() {
        long totalRequests = hits.get() + misses.get();
        if (totalRequests == 0) {
            return 0.0;
        }
        return (hits.get() * 100.0) / totalRequests;
    }

    /**
     * Generate cache key from code and variables.
     * Uses SHA-256 hash for collision resistance.
     *
     * @param code      Python code
     * @param variables Variables map
     * @return Cache key (hex string)
     */
    private String generateKey(String code, Map<String, Object> variables) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");

            // Hash code
            digest.update(code.getBytes());

            // Hash variables (sorted by key for determinism)
            if (variables != null && !variables.isEmpty()) {
                variables.entrySet().stream()
                        .sorted(Map.Entry.comparingByKey())
                        .forEach(entry -> {
                            digest.update(entry.getKey().getBytes());
                            String value = String.valueOf(entry.getValue());
                            digest.update(value.getBytes());
                        });
            }

            byte[] hash = digest.digest();
            return bytesToHex(hash);

        } catch (NoSuchAlgorithmException e) {
            // Fallback to simple hash if SHA-256 not available
            logger.warn("SHA-256 not available, using fallback hash", e);
            return String.valueOf(code.hashCode()) + "_" +
                   String.valueOf(variables != null ? variables.hashCode() : 0);
        }
    }

    /**
     * Convert byte array to hex string.
     */
    private String bytesToHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }

    /**
     * Check if cache entry is expired.
     */
    private boolean isExpired(CacheEntry entry) {
        if (ttlMillis == 0) {
            return false; // No expiration
        }
        long age = System.currentTimeMillis() - entry.createTime;
        return age > ttlMillis;
    }

    /**
     * Cache entry wrapper.
     */
    private static class CacheEntry {
        final Python3Result result;
        final long createTime;

        CacheEntry(Python3Result result) {
            this.result = result;
            this.createTime = System.currentTimeMillis();
        }
    }
}
