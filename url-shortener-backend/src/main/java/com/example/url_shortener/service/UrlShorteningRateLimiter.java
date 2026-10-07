package com.example.url_shortener.service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.List;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Component;

@Component
public class UrlShorteningRateLimiter {

    private static final int MAX_REQUESTS = 10;
    private static final int WINDOW_SECONDS = 60;
    private static final String KEY_PREFIX = "rate-limit:urlshortener:posturl:";
    private static final DefaultRedisScript<Long> INCREMENT_COUNTER = new DefaultRedisScript<>(
            "local count = redis.call('INCR', KEYS[1]); "
                    + "if count == 1 then redis.call('EXPIRE', KEYS[1], ARGV[1]); end; "
                    + "return count;",
            Long.class);

    private final StringRedisTemplate redisTemplate;

    public UrlShorteningRateLimiter(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    public boolean isAllowed(String clientIp) {
        Long requestCount = redisTemplate.execute(
                INCREMENT_COUNTER,
                List.of(KEY_PREFIX + hashClientIp(clientIp)),
                Integer.toString(WINDOW_SECONDS));
        if (requestCount == null) {
            throw new IllegalStateException("Redis did not return a rate-limit counter value");
        }
        return requestCount <= MAX_REQUESTS;
    }

    private String hashClientIp(String clientIp) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest(clientIp.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is unavailable", exception);
        }
    }
}
