package com.vladko.autoshopcore.parts.exception;

public class OrderRequestedPartNotFoundException extends RuntimeException {
    public OrderRequestedPartNotFoundException(Integer orderId, Integer requestedPartId) {
        super("Requested part with id '%s' was not found in order '%s'".formatted(requestedPartId, orderId));
    }
}
