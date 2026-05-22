package com.vladko.autoshopcore.event.notification;

import java.time.Instant;

public record OrderCreatedNotificationPayload(
        Long orderId,
        String orderNumber,
        Long customerId,
        String customerFirstName,
        String customerLastName,
        String customerEmail,
        Long vehicleId,
        String vehicleBrand,
        String vehicleModel,
        String vehiclePlateNumber,
        Instant createdAt
) {
}
