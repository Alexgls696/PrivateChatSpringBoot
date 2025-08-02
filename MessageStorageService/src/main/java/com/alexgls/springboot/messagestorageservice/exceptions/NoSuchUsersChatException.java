package com.alexgls.springboot.messagestorageservice.exceptions;

public class NoSuchUsersChatException extends RuntimeException {
    public NoSuchUsersChatException(String message) {
        super(message);
    }
}
