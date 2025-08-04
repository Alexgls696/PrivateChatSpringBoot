package com.alexgls.springboot.exception;

public class NoSuchImageException extends RuntimeException {
    public NoSuchImageException(String message) {
        super(message);
    }
}
