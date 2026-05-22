package com.vladko.autoshopcore.order.exception;

public class OrderConflictException extends RuntimeException {

    public OrderConflictException(String message) {
        super(message);
    }
}
