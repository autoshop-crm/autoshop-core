package com.vladko.autoshopcore.loyalty.exception;

public class LoyaltyAccountNotFoundException extends RuntimeException {

    public LoyaltyAccountNotFoundException(Integer id) {
        super("Loyalty account with id '%s' was not found".formatted(id));
    }
}
