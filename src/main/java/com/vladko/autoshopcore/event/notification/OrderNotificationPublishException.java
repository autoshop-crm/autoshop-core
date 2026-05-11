package com.vladko.autoshopcore.event.notification;

public class OrderNotificationPublishException extends RuntimeException {

    public OrderNotificationPublishException(String message, Throwable cause) {
        super(message, cause);
    }
}
