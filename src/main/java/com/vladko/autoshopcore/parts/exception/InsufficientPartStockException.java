package com.vladko.autoshopcore.parts.exception;

public class InsufficientPartStockException extends RuntimeException {

    public InsufficientPartStockException(String message) {
        super(message);
    }
}
