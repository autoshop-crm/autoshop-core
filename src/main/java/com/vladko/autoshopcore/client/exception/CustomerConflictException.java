package com.vladko.autoshopcore.client.exception;

public class CustomerConflictException extends RuntimeException {

    public CustomerConflictException(String message) {
        super(message);
    }
}
