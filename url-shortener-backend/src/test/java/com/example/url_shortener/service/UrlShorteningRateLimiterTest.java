package com.example.url_shortener.service;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.RedisScript;

import com.example.url_shortener.config.RateLimitProperties;
import com.example.url_shortener.exception.RateLimitUnavailableException;

@ExtendWith(MockitoExtension.class)
class UrlShorteningRateLimiterTest {

    @Mock
    private StringRedisTemplate redisTemplate;

    private UrlShorteningRateLimiter rateLimiter;

    private final AtomicLong now = new AtomicLong();
    private final Map<String, Counter> counters = new HashMap<>();

    @BeforeEach
    void setUp() {
        RateLimitProperties properties = new RateLimitProperties();
        rateLimiter = new UrlShorteningRateLimiter(redisTemplate, properties);
        lenient().when(redisTemplate.execute(any(RedisScript.class), anyList(), any(Object[].class)))
                .thenAnswer(invocation -> executeCounter(
                        invocation.getArgument(0),
                        invocation.getArgument(1),
                        invocation.getArgument(2)));
    }

    @Test
    void allowsTenUrlCreationRequestsAndRejectsTheEleventh() {
        for (int request = 0; request < 10; request++) {
            assertTrue(rateLimiter.isUrlCreationAllowed("192.0.2.10"));
        }
        assertFalse(rateLimiter.isUrlCreationAllowed("192.0.2.10"));
    }

    @Test
    void usesIndependentConfigAndRedisNamespaceForRedirects() {
        RateLimitProperties properties = new RateLimitProperties();
        properties.setRedirect(new RateLimitProperties.Limit(2, 60));
        rateLimiter = new UrlShorteningRateLimiter(redisTemplate, properties);

        assertTrue(rateLimiter.isRedirectAllowed("192.0.2.10"));
        assertTrue(rateLimiter.isRedirectAllowed("192.0.2.10"));
        assertFalse(rateLimiter.isRedirectAllowed("192.0.2.10"));
        assertTrue(rateLimiter.isUrlCreationAllowed("192.0.2.10"));

        assertTrue(counters.keySet().stream()
                .anyMatch(key -> key.startsWith("rate-limit:urlshortener:redirect:")));
        assertTrue(counters.keySet().stream()
                .anyMatch(key -> key.startsWith("rate-limit:urlshortener:posturl:")));
    }

    @Test
    void requestsOnSeparateInstancesShareTheSameRedisCounter() {
        RateLimitProperties properties = new RateLimitProperties();
        properties.setUrlCreation(new RateLimitProperties.Limit(10, 60));
        UrlShorteningRateLimiter secondInstance = new UrlShorteningRateLimiter(redisTemplate, properties);

        for (int request = 0; request < 6; request++) {
            assertTrue(rateLimiter.isUrlCreationAllowed("192.0.2.10"));
        }
        for (int request = 0; request < 4; request++) {
            assertTrue(secondInstance.isUrlCreationAllowed("192.0.2.10"));
        }
        assertFalse(secondInstance.isUrlCreationAllowed("192.0.2.10"));
    }

    @Test
    void givesDifferentClientsIndependentCounters() {
        assertTrue(rateLimiter.isUrlCreationAllowed("192.0.2.10"));
        assertTrue(rateLimiter.isUrlCreationAllowed("192.0.2.11"));
        assertEquals(2, counters.size());
        assertTrue(counters.keySet().stream().noneMatch(key -> key.contains("192.0.2.")));
    }

    @Test
    void expiredCounterStartsANewWindowWithoutSleeping() {
        RateLimitProperties properties = new RateLimitProperties();
        properties.setUrlCreation(new RateLimitProperties.Limit(1, 1));
        rateLimiter = new UrlShorteningRateLimiter(redisTemplate, properties);

        assertTrue(rateLimiter.isUrlCreationAllowed("192.0.2.10"));
        assertFalse(rateLimiter.isUrlCreationAllowed("192.0.2.10"));
        now.addAndGet(1_000);
        assertTrue(rateLimiter.isUrlCreationAllowed("192.0.2.10"));
    }

    @Test
    void failsClosedWhenRedisIsUnavailable() {
        when(redisTemplate.execute(any(RedisScript.class), anyList(), any(Object[].class)))
                .thenThrow(new DataAccessResourceFailureException("Redis unavailable"));

        assertThrows(RateLimitUnavailableException.class,
                () -> rateLimiter.isUrlCreationAllowed("192.0.2.10"));
    }

    @Test
    void failsClosedWhenRedisReturnsNoCounter() {
        when(redisTemplate.execute(any(RedisScript.class), anyList(), any(Object[].class)))
                .thenReturn(null);

        assertThrows(RateLimitUnavailableException.class,
                () -> rateLimiter.isUrlCreationAllowed("192.0.2.10"));
    }

    private Long executeCounter(RedisScript<Long> script, List<String> keys, String windowSeconds) {
        String lua = script.getScriptAsString();
        assertTrue(lua.contains("redis.call('INCR'"));
        assertTrue(lua.contains("redis.call('EXPIRE'"));
        assertTrue(lua.contains("if count == 1"));

        String key = keys.get(0);
        long windowMillis = Long.parseLong(windowSeconds) * 1_000;
        Counter counter = counters.get(key);
        if (counter == null || counter.expiresAt <= now.get()) {
            counter = new Counter(0, now.get() + windowMillis);
            counters.put(key, counter);
        }
        counter.count++;
        return counter.count;
    }

    private static class Counter {
        private long count;
        private final long expiresAt;

        private Counter(long count, long expiresAt) {
            this.count = count;
            this.expiresAt = expiresAt;
        }
    }
}
