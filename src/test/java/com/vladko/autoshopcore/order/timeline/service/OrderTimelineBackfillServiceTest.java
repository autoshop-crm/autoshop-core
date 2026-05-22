package com.vladko.autoshopcore.order.timeline.service;

import com.vladko.autoshopcore.order.entity.CancellationReason;
import com.vladko.autoshopcore.order.entity.Order;
import com.vladko.autoshopcore.order.entity.OrderStatus;
import com.vladko.autoshopcore.order.timeline.entity.OrderTimelineEventType;
import com.vladko.autoshopcore.order.timeline.entity.OrderTimelineVisibility;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class OrderTimelineBackfillServiceTest {

    private OrderTimelineService orderTimelineService;
    private OrderTimelineBackfillService backfillService;

    @BeforeEach
    void setUp() {
        orderTimelineService = mock(OrderTimelineService.class);
        backfillService = new OrderTimelineBackfillService(orderTimelineService);
    }

    @Test
    void shouldBackfillOnlyKnownFactsAndMarkThemLegacyDerived() {
        Order order = Order.builder()
                .id(41)
                .status(OrderStatus.CANCELLED_NO_SHOW)
                .createdAt(Instant.parse("2026-04-01T08:00:00Z"))
                .checkedInAt(Instant.parse("2026-04-01T08:30:00Z"))
                .cancelledAt(Instant.parse("2026-04-01T10:00:00Z"))
                .cancellationReason(CancellationReason.NO_SHOW)
                .build();

        backfillService.backfill(order);

        verify(orderTimelineService, times(3)).append(any(), any(), any(), any(), any(), any(), anyString(), contains("legacyDerived"), anyString());
    }

    @Test
    void shouldNotFabricateHandoverFromCompletedAt() {
        Order order = Order.builder()
                .id(42)
                .status(OrderStatus.COMPLETED)
                .createdAt(Instant.parse("2026-04-01T08:00:00Z"))
                .completedAt(Instant.parse("2026-04-01T12:00:00Z"))
                .build();

        backfillService.backfill(order);

        ArgumentCaptor<OrderTimelineEventType> eventCaptor = ArgumentCaptor.forClass(OrderTimelineEventType.class);
        ArgumentCaptor<OrderTimelineVisibility> visibilityCaptor = ArgumentCaptor.forClass(OrderTimelineVisibility.class);
        verify(orderTimelineService, times(2)).append(any(), eventCaptor.capture(), visibilityCaptor.capture(), any(), any(), any(), anyString(), anyString(), anyString());

        assertThat(eventCaptor.getAllValues()).doesNotContain(OrderTimelineEventType.VEHICLE_HANDED_OVER);
        assertThat(visibilityCaptor.getAllValues()).contains(OrderTimelineVisibility.STAFF_ONLY);
    }
}
