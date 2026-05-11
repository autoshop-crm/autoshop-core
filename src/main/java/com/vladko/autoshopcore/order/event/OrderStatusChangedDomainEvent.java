package com.vladko.autoshopcore.order.event;

import com.vladko.autoshopcore.event.notification.OrderStatusChangedNotificationPayload;

public record OrderStatusChangedDomainEvent(OrderStatusChangedNotificationPayload payload) {
}
