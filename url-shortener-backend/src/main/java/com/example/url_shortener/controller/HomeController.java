package com.example.url_shortener.controller;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.time.LocalDateTime;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

import com.example.url_shortener.dtos.Urldto;
import com.example.url_shortener.dtos.UrlResponseDTO;
import com.example.url_shortener.exception.ColumnAliasAlreadyExistsException;
import com.example.url_shortener.exception.ErrorResponse;
import com.example.url_shortener.exception.UrlAlreadyExistsException;
import com.example.url_shortener.repository.UrlRepository;
import com.example.url_shortener.service.UrlService;

import jakarta.servlet.http.HttpServletResponse;

@RestController
@RequestMapping("/urlshortener")
public class HomeController {
	
	@Autowired
	UrlService urlService;
	
	@Autowired
	private UrlRepository urlRepository;
	
	@Autowired
	private RestClient restClient;
//	@GetMapping("{shortcode}")
//	public Response
	
	@GetMapping("/home")
	public String home() {
		return "Works !!";
	}
	
	@PostMapping("/posturl")
	public ResponseEntity<?> generateShortCode(@RequestBody Urldto url) {
		UrlResponseDTO savedurl;
				try {
					if(!isValid(url.getUrl())) {
						ErrorResponse error = new ErrorResponse(LocalDateTime.now(), "Url should start with 'http:' or 'https:'", "Url InValid");
						return new ResponseEntity<>(error, HttpStatus.NOT_FOUND);
					}
					if(!isUrlReachable(url.getUrl())) {
						ErrorResponse error = new ErrorResponse(LocalDateTime.now(), "Url is not reachable", "Please provide a valid URL");
						return new ResponseEntity<>(error, HttpStatus.NOT_FOUND);
					}
					
				}
				catch(URISyntaxException e) {
					ErrorResponse invalidurlexception = new ErrorResponse(LocalDateTime.now(), e.getMessage(), "Url Syntax is InValid");
					return new ResponseEntity<>(invalidurlexception, HttpStatus.NOT_FOUND);
				}
				try {
					savedurl = urlService.generateShortCode(url);
					String shortenedUrl = "http://localhost:8080/urlshortener/" + savedurl.getShortCode();
					return new ResponseEntity<>(shortenedUrl, HttpStatus.OK);
				}
				catch(ColumnAliasAlreadyExistsException e) {
					ErrorResponse invalidurlerror = new ErrorResponse(LocalDateTime.now(), e.getMessage(), "Column Alias Already Exists Please choose another one");
					return new ResponseEntity<>(invalidurlerror, HttpStatus.BAD_REQUEST);
				}
				catch(UrlAlreadyExistsException e) {
					ErrorResponse error = new ErrorResponse(LocalDateTime.now(), e.getMessage(), "URL Already Exists Here is the short Code");
					return new ResponseEntity<>(error, HttpStatus.BAD_REQUEST);
				}
	}
	
	@GetMapping("/{shortCode}")
	public void redirect(
	        @PathVariable String shortCode,
	        HttpServletResponse response) throws IOException {

	    String originalUrl = urlService.getOriginalUrl(shortCode);

	    urlService.incrementClickCount(shortCode);

	    response.sendRedirect(originalUrl);
	}
	
	
	private boolean isValid(String url) throws URISyntaxException {
		URI uri = new URI(url);
		return ("http".equalsIgnoreCase(uri.getScheme())
		        || "https".equalsIgnoreCase(uri.getScheme()))
		       && uri.getHost() != null;
	}
	private boolean isUrlReachable(String url) {

	    try {

	        ResponseEntity<Void> response = restClient
	                .head()
	                .uri(url)
	                .retrieve()
	                .toBodilessEntity();

	        return true;

	    } catch (RestClientResponseException e) {

	        // Server responded with 4xx/5xx.
	        // Therefore the server itself is reachable.
	        return true;

	    } catch (RestClientException e) {

	        // DNS failure, connection failure,
	        // timeout, etc.
	        return false;
	    }
	}
}
