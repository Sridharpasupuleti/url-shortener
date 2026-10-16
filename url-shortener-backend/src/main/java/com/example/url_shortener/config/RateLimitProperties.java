package com.example.url_shortener.config;

import java.util.ArrayList;
import java.util.List;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "app.rate-limit")
public class RateLimitProperties {

    private Limit urlCreation = new Limit(10, 60);
    private Limit redirect = new Limit(100, 60);
    private List<String> trustedProxies = new ArrayList<>();

    public Limit getUrlCreation() {
        return urlCreation;
    }

    public void setUrlCreation(Limit urlCreation) {
        this.urlCreation = urlCreation;
    }

    public Limit getRedirect() {
        return redirect;
    }

    public void setRedirect(Limit redirect) {
        this.redirect = redirect;
    }

    public List<String> getTrustedProxies() {
        return trustedProxies;
    }

    public void setTrustedProxies(List<String> trustedProxies) {
        this.trustedProxies = trustedProxies;
    }

    public static class Limit {

        private int maxRequests;
        private int windowSeconds;

        public Limit() {
        }

        public Limit(int maxRequests, int windowSeconds) {
            setMaxRequests(maxRequests);
            setWindowSeconds(windowSeconds);
        }

        public int getMaxRequests() {
            return maxRequests;
        }

        public void setMaxRequests(int maxRequests) {
            if (maxRequests < 1) {
                throw new IllegalArgumentException("Rate-limit max-requests must be positive");
            }
            this.maxRequests = maxRequests;
        }

        public int getWindowSeconds() {
            return windowSeconds;
        }

        public void setWindowSeconds(int windowSeconds) {
            if (windowSeconds < 1) {
                throw new IllegalArgumentException("Rate-limit window-seconds must be positive");
            }
            this.windowSeconds = windowSeconds;
        }
    }
}
