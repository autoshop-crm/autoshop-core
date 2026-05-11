package com.vladko.autoshopcore.event.notification;

import java.math.BigDecimal;
import java.time.Instant;

public record OrderCompletedNotificationPayload(
        Long orderId,
        String orderNumber,
        Long customerId,
        String customerFirstName,
        String customerLastName,
        String customerEmail,
        Instant completedAt,
        BigDecimal finalAmount,
        String currency,
        Integer loyaltyPointsEarned
) {
}
