package com.vladko.autoshopcore.order.timeline.service;

import com.vladko.autoshopcore.order.entity.Order;
import com.vladko.autoshopcore.order.entity.OrderStatus;
import com.vladko.autoshopcore.order.timeline.dto.OrderTimelineEntryResponseDTO;
import com.vladko.autoshopcore.order.timeline.entity.OrderTimelineActorType;
import com.vladko.autoshopcore.order.timeline.entity.OrderTimelineEventType;
import com.vladko.autoshopcore.order.timeline.entity.OrderTimelineVisibility;

import java.util.List;

public interface OrderTimelineService {
    void append(Order order,
                OrderTimelineEventType eventType,
                OrderTimelineVisibility visibility,
                OrderTimelineActorType actorType,
                Long actorId,
                OrderStatus effectiveStatus,
                String summary,
                String detailsJson,
                String dedupeKey);

    List<OrderTimelineEntryResponseDTO> getCustomerTimeline(Integer orderId);
    List<OrderTimelineEntryResponseDTO> getStaffTimeline(Integer orderId);
}
