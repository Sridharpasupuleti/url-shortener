package com.example.url_shortener.service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.List;

import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Component;

import com.example.url_shortener.config.RateLimitProperties;
import com.example.url_shortener.exception.RateLimitUnavailableException;

@Component
public class UrlShorteningRateLimiter {

    private static final String URL_CREATION_KEY_PREFIX = "rate-limit:urlshortener:posturl:";
    private static final String REDIRECT_KEY_PREFIX = "rate-limit:urlshortener:redirect:";
    private static final DefaultRedisScript<Long> INCREMENT_COUNTER = new DefaultRedisScript<>(
            "local count = redis.call('INCR', KEYS[1]); "
                    + "if count == 1 then redis.call('EXPIRE', KEYS[1], ARGV[1]); end; "
                    + "return count;",
            Long.class);

    private final StringRedisTemplate redisTemplate;
    private final RateLimitProperties properties;

    public UrlShorteningRateLimiter(
            StringRedisTemplate redisTemplate,
            RateLimitProperties properties) {
        this.redisTemplate = redisTemplate;
        this.properties = properties;
    }

    public boolean isUrlCreationAllowed(String clientIp) {
        return isAllowed(clientIp, URL_CREATION_KEY_PREFIX, properties.getUrlCreation());
    }

    public boolean isRedirectAllowed(String clientIp) {
        return isAllowed(clientIp, REDIRECT_KEY_PREFIX, properties.getRedirect());
    }

    private boolean isAllowed(
            String clientIp,
            String keyPrefix,
            RateLimitProperties.Limit limit) {
        final Long requestCount;
        try {
            requestCount = redisTemplate.execute(
                    INCREMENT_COUNTER,
                    List.of(keyPrefix + hashClientIp(clientIp)),
                    Integer.toString(limit.getWindowSeconds()));
        } catch (DataAccessException exception) {
            throw new RateLimitUnavailableException(
                    "Rate limiting is temporarily unavailable; the request was not allowed.",
                    exception);
        }
        if (requestCount == null) {
            throw new RateLimitUnavailableException(
                    "Rate limiting is temporarily unavailable; the request was not allowed.",
                    new IllegalStateException("Redis did not return a rate-limit counter value"));
        }
        return requestCount <= limit.getMaxRequests();
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
