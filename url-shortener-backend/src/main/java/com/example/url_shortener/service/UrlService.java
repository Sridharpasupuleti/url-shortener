package com.example.url_shortener.service;

import org.springframework.stereotype.Service;

import com.example.url_shortener.dtos.Urldto;
import com.example.url_shortener.dtos.UrlResponseDTO;

public interface UrlService {

    UrlResponseDTO generateShortCode(Urldto url);

    UrlResponseDTO findByShortCode(String shortCode);

	String getOriginalUrl(String shortCode);

	void incrementClickCount(String shortCode);

}
