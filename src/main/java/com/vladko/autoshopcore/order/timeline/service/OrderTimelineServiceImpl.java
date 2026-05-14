package com.vladko.autoshopcore.order.timeline.service;

import com.vladko.autoshopcore.order.entity.Order;
import com.vladko.autoshopcore.order.entity.OrderStatus;
import com.vladko.autoshopcore.order.exception.OrderNotFoundException;
import com.vladko.autoshopcore.order.repository.OrderRepository;
import com.vladko.autoshopcore.order.timeline.dto.OrderTimelineEntryResponseDTO;
import com.vladko.autoshopcore.order.timeline.entity.*;
import com.vladko.autoshopcore.order.timeline.repository.OrderTimelineEntryRepository;
import com.vladko.autoshopcore.security.CoreSecurityService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class OrderTimelineServiceImpl implements OrderTimelineService {

    private final OrderTimelineEntryRepository repository;
    private final OrderRepository orderRepository;
    private final CoreSecurityService coreSecurityService;

    @Override
    @Transactional
    public void append(Order order, OrderTimelineEventType eventType, OrderTimelineVisibility visibility, OrderTimelineActorType actorType,
                       Long actorId, OrderStatus effectiveStatus, String summary, String detailsJson, String dedupeKey) {
        if (repository.findByOrderIdAndDedupeKey(order.getId(), dedupeKey).isPresent()) {
            return;
        }
        repository.save(OrderTimelineEntry.builder()
                .order(order)
                .eventType(eventType)
                .visibility(visibility)
                .actorType(actorType)
                .actorId(actorId)
                .effectiveStatus(effectiveStatus)
                .summary(summary)
                .detailsJson(detailsJson)
                .dedupeKey(dedupeKey)
                .build());
    }

    @Override
    @Transactional(readOnly = true)
    public List<OrderTimelineEntryResponseDTO> getCustomerTimeline(Integer orderId) {
        Order order = orderRepository.findById(orderId).orElseThrow(() -> new OrderNotFoundException(orderId));
        coreSecurityService.requireCustomerAccess(order);
        return repository.findAllByOrderIdOrderByOccurredAtAscIdAsc(orderId).stream()
                .filter(entry -> entry.getVisibility() != OrderTimelineVisibility.STAFF_ONLY)
                .map(this::map)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<OrderTimelineEntryResponseDTO> getStaffTimeline(Integer orderId) {
        orderRepository.findById(orderId).orElseThrow(() -> new OrderNotFoundException(orderId));
        coreSecurityService.requireAnyStaff();
        return repository.findAllByOrderIdOrderByOccurredAtAscIdAsc(orderId).stream().map(this::map).toList();
    }

    private OrderTimelineEntryResponseDTO map(OrderTimelineEntry entry) {
        return OrderTimelineEntryResponseDTO.builder()
                .id(entry.getId())
                .eventType(entry.getEventType())
                .actorType(entry.getActorType())
                .actorId(entry.getActorId())
                .effectiveStatus(entry.getEffectiveStatus())
                .summary(entry.getSummary())
                .detailsJson(entry.getDetailsJson())
                .occurredAt(entry.getOccurredAt())
                .build();
    }
}
