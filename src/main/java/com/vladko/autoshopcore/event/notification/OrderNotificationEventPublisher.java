package com.vladko.autoshopcore.event.notification;

public interface OrderNotificationEventPublisher {

    void publishOrderCreated(OrderCreatedNotificationPayload payload);

    void publishOrderStatusChanged(OrderStatusChangedNotificationPayload payload);

    void publishOrderCompleted(OrderCompletedNotificationPayload payload);

    void publishOrderApprovalNeeded(OrderApprovalNeededNotificationPayload payload);

    void publishOrderWaitingForPart(OrderWaitingForPartNotificationPayload payload);

    void publishOrderReadyForOwner(OrderReadyForOwnerNotificationPayload payload);

    void publishOrderCancelled(OrderCancelledNotificationPayload payload);
}
