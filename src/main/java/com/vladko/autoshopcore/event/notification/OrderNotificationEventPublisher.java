package com.vladko.autoshopcore.event.notification;

public interface OrderNotificationEventPublisher {

    void publishOrderCreated(OrderCreatedNotificationPayload payload);

    void publishOrderStatusChanged(OrderStatusChangedNotificationPayload payload);

    void publishOrderCompleted(OrderCompletedNotificationPayload payload);
}
