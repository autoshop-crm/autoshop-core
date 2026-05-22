package com.vladko.autoshopcore.order.timeline.dto;

import com.vladko.autoshopcore.order.entity.OrderStatus;
import com.vladko.autoshopcore.order.timeline.entity.OrderTimelineActorType;
import com.vladko.autoshopcore.order.timeline.entity.OrderTimelineEventType;
import lombok.Builder;
import lombok.Value;

import java.time.Instant;

@Value
@Builder
public class OrderTimelineEntryResponseDTO {
    Integer id;
    OrderTimelineEventType eventType;
    OrderTimelineActorType actorType;
    Long actorId;
    OrderStatus effectiveStatus;
    String summary;
    String detailsJson;
    Instant occurredAt;
}
