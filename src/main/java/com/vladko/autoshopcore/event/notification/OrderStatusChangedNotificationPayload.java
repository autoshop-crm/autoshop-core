package com.vladko.autoshopcore.event.notification;

import java.time.Instant;

public record OrderStatusChangedNotificationPayload(
        Long orderId,
        String orderNumber,
        Long customerId,
        String customerFirstName,
        String customerLastName,
        String customerEmail,
        String previousStatus,
        String newStatus,
        Instant changedAt,
        String managerComment
) {
}
