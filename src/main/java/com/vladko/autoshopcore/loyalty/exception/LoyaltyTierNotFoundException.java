package com.vladko.autoshopcore.loyalty.exception;

public class LoyaltyTierNotFoundException extends RuntimeException {

    public LoyaltyTierNotFoundException(String tierName) {
        super("Loyalty tier '%s' was not found".formatted(tierName));
    }
}
