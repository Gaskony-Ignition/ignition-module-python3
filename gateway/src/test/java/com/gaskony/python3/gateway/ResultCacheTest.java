package com.gaskony.python3.gateway;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for ResultCache.
 *
 * Tests caching behavior, eviction policies, and cache statistics.
 *
 * @since v2.16.0 (Phase 3 Week 3-4)
 */
public class ResultCacheTest {

    private ResultCache cache;

    @BeforeEach
    public void setUp() {
        cache = new ResultCache(10, 5000); // 10 entries, 5 second TTL
    }

    @Test
    public void testCacheMiss() {
        Python3Result result = cache.get("print('hello')", new HashMap<>());
        assertNull(result, "Cache should be empty initially");

        Map<String, Object> stats = cache.getStats();
        assertEquals(0L, stats.get("hits"));
        assertEquals(1L, stats.get("misses"));
        assertEquals(0.0, cache.getHitRate(), 0.01);
    }

    @Test
    public void testCacheHit() {
        String code = "result = x + y";
        Map<String, Object> variables = new HashMap<>();
        variables.put("x", 10);
        variables.put("y", 20);

        Python3Result original = new Python3Result(true, 30, null, null);

        // Put in cache
        cache.put(code, variables, original);

        // Get from cache
        Python3Result cached = cache.get(code, variables);

        assertNotNull(cached);
        assertEquals(original.isSuccess(), cached.isSuccess());
        assertEquals(original.getResult(), cached.getResult());

        Map<String, Object> stats = cache.getStats();
        assertEquals(1L, stats.get("hits"));
        assertEquals(0L, stats.get("misses"));
        assertEquals(100.0, cache.getHitRate(), 0.01);
    }

    @Test
    public void testCacheKeyDeterminism() {
        String code = "result = x + y";
        Map<String, Object> vars1 = new HashMap<>();
        vars1.put("x", 10);
        vars1.put("y", 20);

        Map<String, Object> vars2 = new HashMap<>();
        vars2.put("x", 10);
        vars2.put("y", 20);

        Python3Result result = new Python3Result(true, 30, null, null);

        // Put with vars1
        cache.put(code, vars1, result);

        // Get with vars2 (should hit because keys are identical)
        Python3Result cached = cache.get(code, vars2);

        assertNotNull(cached, "Should get cache hit with identical variables");
        assertEquals(result.getResult(), cached.getResult());
    }

    @Test
    public void testCacheKeyDifference_DifferentCode() {
        Map<String, Object> variables = new HashMap<>();
        variables.put("x", 10);

        Python3Result result1 = new Python3Result(true, 1, null, null);
        Python3Result result2 = new Python3Result(true, 2, null, null);

        cache.put("result = x + 1", variables, result1);
        cache.put("result = x + 2", variables, result2);

        Python3Result cached1 = cache.get("result = x + 1", variables);
        Python3Result cached2 = cache.get("result = x + 2", variables);

        assertNotNull(cached1);
        assertNotNull(cached2);
        assertEquals(1, cached1.getResult());
        assertEquals(2, cached2.getResult());
    }

    @Test
    public void testCacheKeyDifference_DifferentVariables() {
        String code = "result = x";

        Map<String, Object> vars1 = new HashMap<>();
        vars1.put("x", 10);

        Map<String, Object> vars2 = new HashMap<>();
        vars2.put("x", 20);

        Python3Result result1 = new Python3Result(true, 10, null, null);
        Python3Result result2 = new Python3Result(true, 20, null, null);

        cache.put(code, vars1, result1);
        cache.put(code, vars2, result2);

        Python3Result cached1 = cache.get(code, vars1);
        Python3Result cached2 = cache.get(code, vars2);

        assertNotNull(cached1);
        assertNotNull(cached2);
        assertEquals(10, cached1.getResult());
        assertEquals(20, cached2.getResult());
    }

    @Test
    public void testNullVariables() {
        String code = "result = 42";
        Python3Result result = new Python3Result(true, 42, null, null);

        cache.put(code, null, result);

        Python3Result cached = cache.get(code, null);
        assertNotNull(cached);
        assertEquals(42, cached.getResult());
    }

    @Test
    public void testEmptyVariables() {
        String code = "result = 42";
        Python3Result result = new Python3Result(true, 42, null, null);

        cache.put(code, new HashMap<>(), result);

        Python3Result cached = cache.get(code, new HashMap<>());
        assertNotNull(cached);
        assertEquals(42, cached.getResult());
    }

    @Test
    public void testNullResultNotCached() {
        String code = "print('hello')";
        cache.put(code, new HashMap<>(), null);

        Python3Result cached = cache.get(code, new HashMap<>());
        assertNull(cached, "Null results should not be cached");

        assertEquals(0, cache.getStats().get("size"));
    }

    @Test
    public void testLRUEviction() {
        cache = new ResultCache(3, 0); // Max 3 entries, no TTL

        // Fill cache
        for (int i = 0; i < 3; i++) {
            cache.put("code" + i, new HashMap<>(), new Python3Result(true, i, null, null));
        }

        assertEquals(3, cache.getStats().get("size"));

        // Access entry 0 and 1 (making entry 2 the LRU)
        cache.get("code0", new HashMap<>());
        cache.get("code1", new HashMap<>());

        // Add entry 3 (should evict entry 2)
        cache.put("code3", new HashMap<>(), new Python3Result(true, 3, null, null));

        assertEquals(3, cache.getStats().get("size"));
        assertEquals(1L, cache.getStats().get("evictions"));

        // Entry 2 should be evicted
        assertNull(cache.get("code2", new HashMap<>()));

        // Entries 0, 1, and 3 should still be cached
        assertNotNull(cache.get("code0", new HashMap<>()));
        assertNotNull(cache.get("code1", new HashMap<>()));
        assertNotNull(cache.get("code3", new HashMap<>()));
    }

    @Test
    public void testTTLExpiration() throws InterruptedException {
        cache = new ResultCache(10, 100); // 100ms TTL

        Python3Result result = new Python3Result(true, 42, null, null);
        cache.put("code", new HashMap<>(), result);

        // Should be cached immediately
        assertNotNull(cache.get("code", new HashMap<>()));

        // intentional fixed delay — TTL expiration is wall-clock based; the cache
        // compares System.currentTimeMillis() to the entry's stored timestamp.
        Thread.sleep(150);

        // Should be expired
        assertNull(cache.get("code", new HashMap<>()));

        Map<String, Object> stats = cache.getStats();
        assertEquals(1L, stats.get("evictions"), "Expired entry should count as eviction");
    }

    @Test
    public void testNoTTLExpiration() throws InterruptedException {
        cache = new ResultCache(10, 0); // No TTL

        Python3Result result = new Python3Result(true, 42, null, null);
        cache.put("code", new HashMap<>(), result);

        // intentional fixed delay — proves that with TTL=0 the entry is *not* evicted
        // even after time passes; the wait itself is the test.
        Thread.sleep(100);

        // Should still be cached
        assertNotNull(cache.get("code", new HashMap<>()));
    }

    @Test
    public void testClear() {
        // Add multiple entries
        for (int i = 0; i < 5; i++) {
            cache.put("code" + i, new HashMap<>(), new Python3Result(true, i, null, null));
        }

        assertEquals(5, cache.getStats().get("size"));

        // Clear cache
        cache.clear();

        assertEquals(0, cache.getStats().get("size"));

        // All entries should be gone
        for (int i = 0; i < 5; i++) {
            assertNull(cache.get("code" + i, new HashMap<>()));
        }
    }

    @Test
    public void testStats() {
        cache = new ResultCache(100, 5000);

        // Initial stats
        Map<String, Object> stats = cache.getStats();
        assertEquals(0, stats.get("size"));
        assertEquals(100, stats.get("maxSize"));
        assertEquals(0L, stats.get("hits"));
        assertEquals(0L, stats.get("misses"));
        assertEquals(0L, stats.get("evictions"));
        assertEquals(0.0, stats.get("hitRate"));
        assertEquals(5000L, stats.get("ttlMillis"));

        // Add some entries
        for (int i = 0; i < 5; i++) {
            cache.put("code" + i, new HashMap<>(), new Python3Result(true, i, null, null));
        }

        stats = cache.getStats();
        assertEquals(5, stats.get("size"));

        // Generate hits and misses
        cache.get("code0", new HashMap<>()); // Hit
        cache.get("code1", new HashMap<>()); // Hit
        cache.get("code99", new HashMap<>()); // Miss

        stats = cache.getStats();
        assertEquals(2L, stats.get("hits"));
        assertEquals(1L, stats.get("misses"));
        assertEquals(66.67, (Double) stats.get("hitRate"), 0.01);
    }

    @Test
    public void testHitRateCalculation() {
        // 0 requests
        assertEquals(0.0, cache.getHitRate(), 0.01);

        // All hits
        cache.put("code1", new HashMap<>(), new Python3Result(true, 1, null, null));
        cache.get("code1", new HashMap<>()); // Hit
        cache.get("code1", new HashMap<>()); // Hit
        assertEquals(100.0, cache.getHitRate(), 0.01);

        // Mixed hits and misses
        cache.get("code2", new HashMap<>()); // Miss
        cache.get("code3", new HashMap<>()); // Miss
        // Total: 2 hits, 2 misses = 50%
        assertEquals(50.0, cache.getHitRate(), 0.01);
    }

    @Test
    public void testVariableOrdering() {
        String code = "result = x + y";

        Map<String, Object> vars1 = new HashMap<>();
        vars1.put("x", 10);
        vars1.put("y", 20);

        Map<String, Object> vars2 = new HashMap<>();
        vars2.put("y", 20); // Different order
        vars2.put("x", 10);

        Python3Result result = new Python3Result(true, 30, null, null);

        cache.put(code, vars1, result);

        // Should get cache hit even with different variable order
        Python3Result cached = cache.get(code, vars2);
        assertNotNull(cached, "Variable order should not affect cache key");
        assertEquals(30, cached.getResult());
    }

    @Test
    public void testConstructorValidation() {
        // Valid
        assertDoesNotThrow(() -> new ResultCache(1, 0));
        assertDoesNotThrow(() -> new ResultCache(1000, 5000));

        // Invalid max size
        assertThrows(IllegalArgumentException.class, () -> new ResultCache(0, 5000));
        assertThrows(IllegalArgumentException.class, () -> new ResultCache(-1, 5000));

        // Invalid TTL
        assertThrows(IllegalArgumentException.class, () -> new ResultCache(10, -1));
    }

    @Test
    public void testConcurrentAccess() throws InterruptedException {
        cache = new ResultCache(100, 0);

        // Simulate concurrent access
        Thread[] threads = new Thread[10];
        for (int i = 0; i < threads.length; i++) {
            final int threadId = i;
            threads[i] = new Thread(() -> {
                for (int j = 0; j < 10; j++) {
                    String code = "code" + (threadId * 10 + j);
                    cache.put(code, new HashMap<>(), new Python3Result(true, j, null, null));
                    cache.get(code, new HashMap<>());
                }
            });
            threads[i].start();
        }

        // Wait for all threads
        for (Thread thread : threads) {
            thread.join();
        }

        // Should have 100 entries (no errors)
        assertEquals(100, cache.getStats().get("size"));
    }
}
