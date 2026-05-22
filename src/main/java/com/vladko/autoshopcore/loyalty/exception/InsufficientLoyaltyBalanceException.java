package com.vladko.autoshopcore.loyalty.exception;

public class InsufficientLoyaltyBalanceException extends RuntimeException {

    public InsufficientLoyaltyBalanceException(String message) {
        super(message);
    }
}
