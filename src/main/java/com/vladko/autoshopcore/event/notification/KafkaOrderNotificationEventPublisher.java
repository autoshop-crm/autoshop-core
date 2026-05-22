package com.vladko.autoshopcore.event.notification;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "app.events", name = "order-notifications-enabled", havingValue = "true", matchIfMissing = true)
public class KafkaOrderNotificationEventPublisher implements OrderNotificationEventPublisher {

    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;
    private final OrderNotificationEventProperties properties;

    @Value("${app.kafka.order-events-topic}")
    private String topic;

    @Override
    public void publishOrderCreated(OrderCreatedNotificationPayload payload) {
        publish(OrderNotificationEventType.ORDER_CREATED, payload.orderId(), payload);
    }

    @Override
    public void publishOrderStatusChanged(OrderStatusChangedNotificationPayload payload) {
        publish(OrderNotificationEventType.ORDER_STATUS_CHANGED, payload.orderId(), payload);
    }

    @Override
    public void publishOrderCompleted(OrderCompletedNotificationPayload payload) {
        publish(OrderNotificationEventType.ORDER_COMPLETED, payload.orderId(), payload);
    }

    @Override
    public void publishOrderApprovalNeeded(OrderApprovalNeededNotificationPayload payload) {
        publish(OrderNotificationEventType.ORDER_APPROVAL_NEEDED, payload.orderId(), payload);
    }

    @Override
    public void publishOrderWaitingForPart(OrderWaitingForPartNotificationPayload payload) {
        publish(OrderNotificationEventType.ORDER_WAITING_FOR_PART, payload.orderId(), payload);
    }

    @Override
    public void publishOrderReadyForOwner(OrderReadyForOwnerNotificationPayload payload) {
        publish(OrderNotificationEventType.ORDER_READY_FOR_OWNER, payload.orderId(), payload);
    }

    @Override
    public void publishOrderCancelled(OrderCancelledNotificationPayload payload) {
        publish(OrderNotificationEventType.ORDER_CANCELLED, payload.orderId(), payload);
    }

    private void publish(OrderNotificationEventType eventType, Long orderId, Object payload) {
        UUID eventId = UUID.randomUUID();
        NotificationEventEnvelope envelope = new NotificationEventEnvelope(
                eventId,
                eventType.name(),
                Instant.now(),
                properties.source(),
                properties.version(),
                "order-%d-%s".formatted(orderId, eventType.correlationSuffix()),
                payload
        );

        String json;
        try {
            json = objectMapper.writeValueAsString(envelope);
        } catch (JsonProcessingException ex) {
            throw new OrderNotificationPublishException(
                    "Failed to serialize order notification event %s for order %d".formatted(eventType, orderId),
                    ex
            );
        }

        kafkaTemplate.send(topic, eventId.toString(), json)
                .whenComplete((result, ex) -> {
                    if (ex != null) {
                        log.error(
                                "Failed to publish order notification event eventId={} eventType={} orderId={} topic={}",
                                eventId,
                                eventType,
                                orderId,
                                topic,
                                ex
                        );
                        return;
                    }

                    log.info(
                            "Published order notification event eventId={} eventType={} orderId={} topic={}",
                            eventId,
                            eventType,
                            orderId,
                            topic
                    );
                });
    }
}
