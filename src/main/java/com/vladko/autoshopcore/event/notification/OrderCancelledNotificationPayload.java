package com.vladko.autoshopcore.event.notification;

import java.time.Instant;

public record OrderCancelledNotificationPayload(
        Long orderId,
        String orderNumber,
        Long customerId,
        String customerEmail,
        String cancellationReason,
        Instant cancelledAt
) {
}
