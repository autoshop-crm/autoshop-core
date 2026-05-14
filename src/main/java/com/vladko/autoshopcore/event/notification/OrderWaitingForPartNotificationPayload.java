package com.vladko.autoshopcore.event.notification;

import java.time.Instant;

public record OrderWaitingForPartNotificationPayload(
        Long orderId,
        String orderNumber,
        Long customerId,
        String customerEmail,
        Long requestedPartId,
        String partName,
        Instant changedAt
) {
}
