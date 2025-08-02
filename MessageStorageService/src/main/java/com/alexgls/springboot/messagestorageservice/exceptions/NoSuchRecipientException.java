package com.alexgls.springboot.messagestorageservice.exceptions;

public class NoSuchRecipientException extends RuntimeException {
    public NoSuchRecipientException(String message) {
        super(message);
    }
}
