package com.vladko.autoshopcore.order.event;

import com.vladko.autoshopcore.event.notification.OrderNotificationEventPublisher;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Slf4j
@Component
@RequiredArgsConstructor
public class OrderNotificationDomainEventHandler {

    private final OrderNotificationEventPublisher publisher;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handle(OrderCreatedDomainEvent event) {
        try {
            publisher.publishOrderCreated(event.payload());
        } catch (RuntimeException ex) {
            log.error("Failed to handle ORDER_CREATED domain event for orderId={}", event.payload().orderId(), ex);
        }
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handle(OrderStatusChangedDomainEvent event) {
        try {
            publisher.publishOrderStatusChanged(event.payload());
        } catch (RuntimeException ex) {
            log.error("Failed to handle ORDER_STATUS_CHANGED domain event for orderId={}", event.payload().orderId(), ex);
        }
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handle(OrderCompletedDomainEvent event) {
        try {
            publisher.publishOrderCompleted(event.payload());
        } catch (RuntimeException ex) {
            log.error("Failed to handle ORDER_COMPLETED domain event for orderId={}", event.payload().orderId(), ex);
        }
    }
}
