package com.vladko.autoshopcore.event.notification;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@ConditionalOnProperty(prefix = "app.events", name = "order-notifications-enabled", havingValue = "false")
public class NoopOrderNotificationEventPublisher implements OrderNotificationEventPublisher {

    @Override
    public void publishOrderCreated(OrderCreatedNotificationPayload payload) {
        log.debug("Order notification publishing is disabled: skipped ORDER_CREATED for orderId={}", payload.orderId());
    }

    @Override
    public void publishOrderStatusChanged(OrderStatusChangedNotificationPayload payload) {
        log.debug("Order notification publishing is disabled: skipped ORDER_STATUS_CHANGED for orderId={}", payload.orderId());
    }

    @Override
    public void publishOrderCompleted(OrderCompletedNotificationPayload payload) {
        log.debug("Order notification publishing is disabled: skipped ORDER_COMPLETED for orderId={}", payload.orderId());
    }

    @Override
    public void publishOrderApprovalNeeded(OrderApprovalNeededNotificationPayload payload) {
        log.debug("Order notification publishing is disabled: skipped ORDER_APPROVAL_NEEDED for orderId={}", payload.orderId());
    }

    @Override
    public void publishOrderWaitingForPart(OrderWaitingForPartNotificationPayload payload) {
        log.debug("Order notification publishing is disabled: skipped ORDER_WAITING_FOR_PART for orderId={}", payload.orderId());
    }

    @Override
    public void publishOrderReadyForOwner(OrderReadyForOwnerNotificationPayload payload) {
        log.debug("Order notification publishing is disabled: skipped ORDER_READY_FOR_OWNER for orderId={}", payload.orderId());
    }

    @Override
    public void publishOrderCancelled(OrderCancelledNotificationPayload payload) {
        log.debug("Order notification publishing is disabled: skipped ORDER_CANCELLED for orderId={}", payload.orderId());
    }
}
