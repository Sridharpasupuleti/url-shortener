package com.example.url_shortener.exception;

public class ColumnAliasAlreadyExistsException extends RuntimeException {
	public ColumnAliasAlreadyExistsException(String message) {
		super(message);
	}
}
