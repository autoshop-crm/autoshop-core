package com.vladko.autoshopcore.client.exception;

public class CustomerNotFoundException extends RuntimeException {

    public CustomerNotFoundException(Integer id) {
        super("Customer with id '%s' was not found".formatted(id));
    }

    public CustomerNotFoundException(String message) {
        super(message);
    }
}
