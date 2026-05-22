package com.vladko.autoshopcore.order.event;

import com.vladko.autoshopcore.event.notification.OrderCompletedNotificationPayload;
import com.vladko.autoshopcore.event.notification.OrderCreatedNotificationPayload;
import com.vladko.autoshopcore.event.notification.OrderNotificationEventPublisher;
import com.vladko.autoshopcore.event.notification.OrderStatusChangedNotificationPayload;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class OrderNotificationDomainEventHandlerTest {

    private final OrderNotificationEventPublisher publisher = mock(OrderNotificationEventPublisher.class);
    private final OrderNotificationDomainEventHandler handler = new OrderNotificationDomainEventHandler(publisher);

    @Test
    void handleCreatedShouldDelegateToPublisher() {
        OrderCreatedNotificationPayload payload = createdPayload();

        handler.handle(new OrderCreatedDomainEvent(payload));

        verify(publisher).publishOrderCreated(payload);
    }

    @Test
    void handleStatusChangedShouldDelegateToPublisher() {
        OrderStatusChangedNotificationPayload payload = new OrderStatusChangedNotificationPayload(
                42L,
                "AS-2026-00042",
                7L,
                "Ivan",
                "Petrov",
                "ivan@example.com",
                "NEW",
                "IN_PROGRESS",
                Instant.parse("2026-04-22T12:00:00Z"),
                ""
        );

        handler.handle(new OrderStatusChangedDomainEvent(payload));

        verify(publisher).publishOrderStatusChanged(payload);
    }

    @Test
    void handleCompletedShouldDelegateToPublisher() {
        OrderCompletedNotificationPayload payload = new OrderCompletedNotificationPayload(
                42L,
                "AS-2026-00042",
                7L,
                "Ivan",
                "Petrov",
                "ivan@example.com",
                Instant.parse("2026-04-22T17:45:00Z"),
                new BigDecimal("12500.00"),
                "RUB",
                625
        );

        handler.handle(new OrderCompletedDomainEvent(payload));

        verify(publisher).publishOrderCompleted(payload);
    }

    private OrderCreatedNotificationPayload createdPayload() {
        return new OrderCreatedNotificationPayload(
                42L,
                "AS-2026-00042",
                7L,
                "Ivan",
                "Petrov",
                "ivan@example.com",
                12L,
                "Toyota",
                "Camry",
                "A123BC77",
                Instant.parse("2026-04-22T10:15:30Z")
        );
    }
}
