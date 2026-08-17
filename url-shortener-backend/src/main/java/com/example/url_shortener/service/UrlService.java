package com.example.url_shortener.service;

import org.springframework.stereotype.Service;

import com.example.url_shortener.dtos.Urldto;
import com.example.url_shortener.entities.Urls;

public interface UrlService {

	Urls generateShortCode(Urldto url);

	Urls findByShortCode(String shortCode);
	
}
