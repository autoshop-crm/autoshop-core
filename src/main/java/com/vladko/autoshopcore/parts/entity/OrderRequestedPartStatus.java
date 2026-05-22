package com.vladko.autoshopcore.parts.entity;

public enum OrderRequestedPartStatus {
    PENDING_CUSTOMER_APPROVAL,
    OUT_OF_STOCK,
    ORDERED_IN_TRANSIT,
    IN_STOCK_RESERVED,
    INSTALLED,
    CANCELLED
}
