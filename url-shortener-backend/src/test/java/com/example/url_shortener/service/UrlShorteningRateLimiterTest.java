package com.example.url_shortener.service;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.concurrent.atomic.AtomicLong;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.RedisScript;

@ExtendWith(MockitoExtension.class)
class UrlShorteningRateLimiterTest {

    @Mock
    private StringRedisTemplate redisTemplate;

    private UrlShorteningRateLimiter rateLimiter;

    @BeforeEach
    void setUp() {
        rateLimiter = new UrlShorteningRateLimiter(redisTemplate);
    }

    @Test
    void allowsTenRequestsAndRejectsTheNextRequest() {
        AtomicLong counter = new AtomicLong();
        when(redisTemplate.execute(any(RedisScript.class), anyList(), any(Object[].class)))
                .thenAnswer(invocation -> counter.incrementAndGet());

        for (int request = 0; request < 10; request++) {
            assertTrue(rateLimiter.isAllowed("192.0.2.10"));
        }
        assertFalse(rateLimiter.isAllowed("192.0.2.10"));

        ArgumentCaptor<RedisScript> script = ArgumentCaptor.forClass(RedisScript.class);
        ArgumentCaptor<java.util.List<String>> keys = ArgumentCaptor.forClass(java.util.List.class);
        ArgumentCaptor<Object[]> arguments = ArgumentCaptor.forClass(Object[].class);
        verify(redisTemplate, org.mockito.Mockito.times(11))
                .execute(script.capture(), keys.capture(), arguments.capture());
        assertTrue(script.getValue().getScriptAsString().contains("redis.call('INCR'"));
        assertTrue(script.getValue().getScriptAsString().contains("redis.call('EXPIRE'"));
        assertTrue(keys.getValue().get(0).startsWith("rate-limit:urlshortener:posturl:"));
        assertFalse(keys.getValue().get(0).contains("192.0.2.10"));
        org.junit.jupiter.api.Assertions.assertArrayEquals(new Object[] {"60"}, arguments.getValue());
    }

    @Test
    void reportsRedisFailureInsteadOfAllowingRequest() {
        when(redisTemplate.execute(any(RedisScript.class), anyList(), any(Object[].class)))
                .thenReturn(null);

        assertThrows(IllegalStateException.class, () -> rateLimiter.isAllowed("192.0.2.10"));
    }

    @Test
    void usesSeparateRedisCountersForDifferentClientIps() {
        when(redisTemplate.execute(any(RedisScript.class), anyList(), any(Object[].class)))
                .thenReturn(1L);

        assertTrue(rateLimiter.isAllowed("192.0.2.10"));
        assertTrue(rateLimiter.isAllowed("192.0.2.11"));

        ArgumentCaptor<java.util.List<String>> keys = ArgumentCaptor.forClass(java.util.List.class);
        verify(redisTemplate, org.mockito.Mockito.times(2))
                .execute(any(RedisScript.class), keys.capture(), any(Object[].class));
        assertNotEquals(keys.getAllValues().get(0), keys.getAllValues().get(1));
    }
}
