package com.vladko.autoshopcore.event.notification;

public enum OrderNotificationEventType {
    ORDER_CREATED("created"),
    ORDER_STATUS_CHANGED("status-changed"),
    ORDER_COMPLETED("completed"),
    ORDER_APPROVAL_NEEDED("approval-needed"),
    ORDER_WAITING_FOR_PART("waiting-for-part"),
    ORDER_READY_FOR_OWNER("ready-for-owner"),
    ORDER_CANCELLED("cancelled");

    private final String correlationSuffix;

    OrderNotificationEventType(String correlationSuffix) {
        this.correlationSuffix = correlationSuffix;
    }

    String correlationSuffix() {
        return correlationSuffix;
    }
}
