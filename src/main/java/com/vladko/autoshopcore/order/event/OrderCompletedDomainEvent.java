package com.vladko.autoshopcore.order.event;

import com.vladko.autoshopcore.event.notification.OrderCompletedNotificationPayload;

public record OrderCompletedDomainEvent(OrderCompletedNotificationPayload payload) {
}
