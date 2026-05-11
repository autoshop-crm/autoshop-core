package com.vladko.autoshopcore.event.notification;

import java.time.Instant;
import java.util.UUID;

public record NotificationEventEnvelope(
        UUID eventId,
        String eventType,
        Instant occurredAt,
        String source,
        int version,
        String correlationId,
        Object payload
) {
}
