package com.vladko.autoshopcore.order.event;

import com.vladko.autoshopcore.event.notification.OrderCreatedNotificationPayload;

public record OrderCreatedDomainEvent(OrderCreatedNotificationPayload payload) {
}
