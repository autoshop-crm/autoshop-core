package com.vladko.autoshopcore.event.notification;

import java.math.BigDecimal;
import java.time.Instant;

public record OrderReadyForOwnerNotificationPayload(
        Long orderId,
        String orderNumber,
        Long customerId,
        String customerEmail,
        Instant readyAt,
        BigDecimal finalAmount,
        String currency
) {
}
