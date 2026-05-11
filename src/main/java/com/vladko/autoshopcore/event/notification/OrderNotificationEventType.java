package com.vladko.autoshopcore.event.notification;

public enum OrderNotificationEventType {
    ORDER_CREATED("created"),
    ORDER_STATUS_CHANGED("status-changed"),
    ORDER_COMPLETED("completed");

    private final String correlationSuffix;

    OrderNotificationEventType(String correlationSuffix) {
        this.correlationSuffix = correlationSuffix;
    }

    String correlationSuffix() {
        return correlationSuffix;
    }
}
