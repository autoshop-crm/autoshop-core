package com.vladko.autoshopcore.order.exception;

public class OrderNotFoundException extends RuntimeException {

    public OrderNotFoundException(Integer id) {
        super("Order with id '%s' was not found".formatted(id));
    }
}
