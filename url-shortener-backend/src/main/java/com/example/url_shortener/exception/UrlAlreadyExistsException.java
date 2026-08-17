package com.example.url_shortener.exception;



public class UrlAlreadyExistsException extends RuntimeException {
	String message;

	public UrlAlreadyExistsException(String message) {
		super(message);
		this.message = message;
	}
}
