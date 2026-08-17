package com.example.url_shortener.exception;


public class URLNotFoundException extends RuntimeException {
	String message;

	public URLNotFoundException(String message) {
		super(message);
		this.message = message;
	}
}
