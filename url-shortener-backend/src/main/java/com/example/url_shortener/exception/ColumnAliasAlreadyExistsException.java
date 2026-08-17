package com.example.url_shortener.exception;

public class ColumnAliasAlreadyExistsException extends RuntimeException {
	String message;

	public ColumnAliasAlreadyExistsException(String message) {
		super();
		this.message = message;
	}
}
