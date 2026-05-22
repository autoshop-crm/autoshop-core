package com.vladko.autoshopcore.order.timeline.service;

import com.vladko.autoshopcore.client.entity.Customer;
import com.vladko.autoshopcore.order.entity.Order;
import com.vladko.autoshopcore.order.entity.OrderStatus;
import com.vladko.autoshopcore.order.repository.OrderRepository;
import com.vladko.autoshopcore.order.timeline.entity.OrderTimelineActorType;
import com.vladko.autoshopcore.order.timeline.entity.OrderTimelineEntry;
import com.vladko.autoshopcore.order.timeline.entity.OrderTimelineEventType;
import com.vladko.autoshopcore.order.timeline.entity.OrderTimelineVisibility;
import com.vladko.autoshopcore.order.timeline.repository.OrderTimelineEntryRepository;
import com.vladko.autoshopcore.security.CoreSecurityService;
import com.vladko.autoshopcore.vehicle.entity.Vehicle;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OrderTimelineServiceTest {

    @Mock private OrderTimelineEntryRepository repository;
    @Mock private OrderRepository orderRepository;
    @Mock private CoreSecurityService coreSecurityService;

    private OrderTimelineService service;

    @BeforeEach
    void setUp() {
        service = new OrderTimelineServiceImpl(repository, orderRepository, coreSecurityService);
    }

    @Test
    void getCustomerTimelineShouldHideStaffOnlyEntries() {
        Customer customer = Customer.builder().id(1).build();
        Vehicle vehicle = Vehicle.builder().id(2).customer(customer).build();
        Order order = Order.builder().id(5).customer(customer).vehicle(vehicle).status(OrderStatus.WAITING_FOR_PART).build();
        when(orderRepository.findById(5)).thenReturn(Optional.of(order));
        doNothing().when(coreSecurityService).requireCustomerAccess(order);
        when(repository.findAllByOrderIdOrderByOccurredAtAscIdAsc(5)).thenReturn(List.of(
                OrderTimelineEntry.builder().id(1).order(order).eventType(OrderTimelineEventType.PART_ORDERED).visibility(OrderTimelineVisibility.STAFF_ONLY).actorType(OrderTimelineActorType.MANAGER).summary("internal").dedupeKey("1").occurredAt(Instant.now()).build(),
                OrderTimelineEntry.builder().id(2).order(order).eventType(OrderTimelineEventType.WAITING_FOR_PART_ENTERED).visibility(OrderTimelineVisibility.BOTH).actorType(OrderTimelineActorType.MANAGER).summary("waiting").dedupeKey("2").occurredAt(Instant.now()).build()
        ));

        assertThat(service.getCustomerTimeline(5)).hasSize(1);
        assertThat(service.getCustomerTimeline(5).get(0).getEventType()).isEqualTo(OrderTimelineEventType.WAITING_FOR_PART_ENTERED);
    }

    @Test
    void appendShouldBeIdempotentByDedupeKey() {
        Order order = Order.builder().id(5).build();
        when(repository.findByOrderIdAndDedupeKey(5, "dup")).thenReturn(Optional.of(OrderTimelineEntry.builder().id(9).build()));

        service.append(order, OrderTimelineEventType.STATUS_CHANGED, OrderTimelineVisibility.BOTH, OrderTimelineActorType.SYSTEM, null, OrderStatus.ACCEPTED, "msg", null, "dup");

        verify(repository, never()).save(any());
    }
}
