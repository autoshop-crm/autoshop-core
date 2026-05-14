package com.vladko.autoshopcore.order.timeline.service;

import com.vladko.autoshopcore.order.entity.CancellationReason;
import com.vladko.autoshopcore.order.entity.Order;
import com.vladko.autoshopcore.order.timeline.entity.OrderTimelineActorType;
import com.vladko.autoshopcore.order.timeline.entity.OrderTimelineEventType;
import com.vladko.autoshopcore.order.timeline.entity.OrderTimelineVisibility;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

@Service
@RequiredArgsConstructor
public class OrderTimelineBackfillService {

    private final OrderTimelineService orderTimelineService;

    @Transactional
    public void backfill(List<Order> orders) {
        orders.forEach(this::backfill);
    }

    @Transactional
    public void backfill(Order order) {
        append(order, OrderTimelineEventType.STATUS_CHANGED, OrderTimelineVisibility.BOTH, "Legacy order imported into CRM timeline", order.getCreatedAt(), "legacy-import");
        append(order, OrderTimelineEventType.VEHICLE_CHECKED_IN, OrderTimelineVisibility.BOTH, "Legacy factual check-in imported", order.getCheckedInAt(), "legacy-checkin");
        append(order, OrderTimelineEventType.READY_FOR_OWNER_MARKED, OrderTimelineVisibility.BOTH, "Legacy ready-for-owner fact imported", order.getReadyForOwnerAt(), "legacy-ready");
        append(order, OrderTimelineEventType.VEHICLE_HANDED_OVER, OrderTimelineVisibility.BOTH, "Legacy handover fact imported", order.getHandedOverAt(), "legacy-handover");
        if (order.getCancelledAt() != null) {
            String summary = order.getCancellationReason() == CancellationReason.NO_SHOW
                    ? "Legacy no-show cancellation imported"
                    : "Legacy cancellation imported";
            append(order, OrderTimelineEventType.ORDER_CANCELLED, OrderTimelineVisibility.BOTH, summary, order.getCancelledAt(), "legacy-cancelled");
        }
        if (order.getCompletedAt() != null && order.getHandedOverAt() == null) {
            append(order, OrderTimelineEventType.STATUS_CHANGED, OrderTimelineVisibility.STAFF_ONLY,
                    "Legacy completion observed without factual handover; no synthetic handover created",
                    order.getCompletedAt(), "legacy-completed-without-handover");
        }
    }

    private void append(Order order, OrderTimelineEventType eventType, OrderTimelineVisibility visibility,
                        String summary, Instant occurredAt, String keyPrefix) {
        if (order.getId() == null || occurredAt == null) {
            return;
        }
        String detailsJson = "{\"legacyDerived\":true,\"source\":\"phase10-backfill\",\"occurredAt\":\"" + occurredAt + "\"}";
        orderTimelineService.append(order, eventType, visibility, OrderTimelineActorType.SYSTEM, null,
                order.getStatus(), summary, detailsJson, keyPrefix + "-" + order.getId() + "-" + occurredAt);
    }
}
