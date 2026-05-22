package com.vladko.autoshopcore.event.notification;

import java.math.BigDecimal;
import java.time.Instant;

public record OrderApprovalNeededNotificationPayload(
        Long orderId,
        String orderNumber,
        Long customerId,
        String customerEmail,
        Long approvalRequestId,
        String approvalType,
        BigDecimal requestedAmount,
        Instant expiresAt
) {
}
