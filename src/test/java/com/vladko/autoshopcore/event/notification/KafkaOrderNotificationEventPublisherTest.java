package com.vladko.autoshopcore.event.notification;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Instant;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class KafkaOrderNotificationEventPublisherTest {

    private final KafkaTemplate<String, String> kafkaTemplate = mock(KafkaTemplate.class);
    private final ObjectMapper objectMapper = new ObjectMapper()
            .registerModule(new JavaTimeModule())
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
    private final OrderNotificationEventProperties properties =
            new OrderNotificationEventProperties("autoshop-core", 1, true);
    private final KafkaOrderNotificationEventPublisher publisher =
            new KafkaOrderNotificationEventPublisher(kafkaTemplate, objectMapper, properties);

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(publisher, "topic", "autoshop.order-events");
    }

    @Test
    void publishOrderCreatedShouldSendEnvelopeToConfiguredTopic() throws Exception {
        OrderCreatedNotificationPayload payload = new OrderCreatedNotificationPayload(
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
        when(kafkaTemplate.send(
                org.mockito.ArgumentMatchers.eq("autoshop.order-events"),
                org.mockito.ArgumentMatchers.anyString(),
                org.mockito.ArgumentMatchers.anyString()
        )).thenReturn(CompletableFuture.completedFuture(mock(SendResult.class)));

        publisher.publishOrderCreated(payload);

        ArgumentCaptor<String> keyCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> valueCaptor = ArgumentCaptor.forClass(String.class);
        verify(kafkaTemplate).send(
                org.mockito.ArgumentMatchers.eq("autoshop.order-events"),
                keyCaptor.capture(),
                valueCaptor.capture()
        );

        JsonNode root = objectMapper.readTree(valueCaptor.getValue());
        assertThat(UUID.fromString(keyCaptor.getValue())).isEqualTo(UUID.fromString(root.get("eventId").asText()));
        assertThat(root.get("eventType").asText()).isEqualTo("ORDER_CREATED");
        assertThat(root.get("source").asText()).isEqualTo("autoshop-core");
        assertThat(root.get("version").asInt()).isEqualTo(1);
        assertThat(root.get("correlationId").asText()).isEqualTo("order-42-created");
        assertThat(root.get("payload").get("orderNumber").asText()).isEqualTo("AS-2026-00042");
        assertThat(root.get("payload").get("customerEmail").asText()).isEqualTo("ivan@example.com");
    }
}
