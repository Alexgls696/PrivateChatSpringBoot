package com.alexgls.springboot.exception;

public class InDatabaseServiceException extends RuntimeException {
    public InDatabaseServiceException(String message) {
        super(message);
    }
}
