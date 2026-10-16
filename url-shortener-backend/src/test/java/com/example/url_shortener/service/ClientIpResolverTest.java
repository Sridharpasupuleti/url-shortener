package com.example.url_shortener.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;

import com.example.url_shortener.config.RateLimitProperties;

import jakarta.servlet.http.HttpServletRequest;

class ClientIpResolverTest {

    @Test
    void ignoresForwardedHeaderFromUntrustedPeer() {
        RateLimitProperties properties = new RateLimitProperties();
        properties.getTrustedProxies().add("10.0.0.0/8");
        ClientIpResolver resolver = new ClientIpResolver(properties);
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getRemoteAddr()).thenReturn("203.0.113.5");
        when(request.getHeader("X-Forwarded-For")).thenReturn("198.51.100.9");

        assertEquals("203.0.113.5", resolver.resolve(request));
    }

    @Test
    void resolvesClientAddressThroughConfiguredProxyChain() {
        RateLimitProperties properties = new RateLimitProperties();
        properties.getTrustedProxies().add("10.0.0.0/8");
        ClientIpResolver resolver = new ClientIpResolver(properties);
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getRemoteAddr()).thenReturn("10.0.0.3");
        when(request.getHeader("X-Forwarded-For"))
                .thenReturn("198.51.100.9, 10.2.0.4");

        assertEquals("198.51.100.9", resolver.resolve(request));
    }

    @Test
    void fallsBackToProxyAddressForMalformedForwardedHeader() {
        RateLimitProperties properties = new RateLimitProperties();
        properties.getTrustedProxies().add("10.0.0.3");
        ClientIpResolver resolver = new ClientIpResolver(properties);
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getRemoteAddr()).thenReturn("10.0.0.3");
        when(request.getHeader("X-Forwarded-For")).thenReturn("cafe");

        assertEquals("10.0.0.3", resolver.resolve(request));
    }
}
