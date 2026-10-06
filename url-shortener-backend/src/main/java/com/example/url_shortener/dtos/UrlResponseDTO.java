package com.example.url_shortener.dtos;

public class UrlResponseDTO {
    private String originalUrl;
    private String shortCode;

    public UrlResponseDTO() {}

    public UrlResponseDTO(String originalUrl, String shortCode) {
        this.originalUrl = originalUrl;
        this.shortCode = shortCode;
    }

    public String getOriginalUrl() {
        return originalUrl;
    }

    public void setOriginalUrl(String originalUrl) {
        this.originalUrl = originalUrl;
    }

    public String getShortCode() {
        return shortCode;
    }

    public void setShortCode(String shortCode) {
        this.shortCode = shortCode;
    }
}
