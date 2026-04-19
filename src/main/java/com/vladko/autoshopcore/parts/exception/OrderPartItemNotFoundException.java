package com.vladko.autoshopcore.parts.exception;

public class OrderPartItemNotFoundException extends RuntimeException {

    public OrderPartItemNotFoundException(Integer orderId, Integer itemId) {
        super("Order part item with id '%s' was not found for order '%s'".formatted(itemId, orderId));
    }
}
