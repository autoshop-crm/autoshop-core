package com.vladko.autoshopcore.order.query.service;

import com.vladko.autoshopcore.loyalty.service.CrmLoyaltyFacade;
import com.vladko.autoshopcore.order.dto.OrderResponseDTO;
import com.vladko.autoshopcore.order.entity.Order;
import com.vladko.autoshopcore.order.entity.OrderStatus;
import com.vladko.autoshopcore.order.query.dto.OrderQueueSummaryDTO;
import com.vladko.autoshopcore.order.query.dto.OrderSearchResponseDTO;
import com.vladko.autoshopcore.order.repository.OrderRepository;
import com.vladko.autoshopcore.order.service.OrderService;
import com.vladko.autoshopcore.security.CoreSecurityService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

@Service
@RequiredArgsConstructor
public class OrderQueryServiceImpl implements OrderQueryService {

    private final OrderRepository orderRepository;
    private final OrderService orderService;
    private final CoreSecurityService coreSecurityService;
    private final CrmLoyaltyFacade crmLoyaltyFacade;

    @Override
    @Transactional(readOnly = true)
    public OrderSearchResponseDTO search(Integer customerId, Integer vehicleId, OrderStatus status, Integer employeeId,
                                         Instant plannedFrom, Instant plannedTo, String q, int page, int size) {
        coreSecurityService.requireAnyStaff();
        int normalizedPage = Math.max(page, 0);
        int normalizedSize = Math.min(Math.max(size, 1), 100);
        String normalizedQuery = q == null || q.isBlank() ? null : q.trim().toLowerCase();
        List<Order> matches = orderRepository.searchForCrm(customerId, vehicleId, status, employeeId, plannedFrom, plannedTo, normalizedQuery);
        int fromIndex = Math.min(normalizedPage * normalizedSize, matches.size());
        int toIndex = Math.min(fromIndex + normalizedSize, matches.size());
        List<OrderResponseDTO> items = matches.subList(fromIndex, toIndex).stream()
                .map(order -> orderService.getById(order.getId()))
                .toList();
        return OrderSearchResponseDTO.builder()
                .items(items)
                .page(normalizedPage)
                .size(normalizedSize)
                .hasMore(toIndex < matches.size())
                .loyaltySettings(crmLoyaltyFacade.getSettings())
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public OrderQueueSummaryDTO queueSummary() {
        coreSecurityService.requireAnyStaff();
        List<Order> orders = orderRepository.findAll();
        return OrderQueueSummaryDTO.builder()
                .waitingForVisit(count(orders, OrderStatus.WAITING_FOR_VISIT))
                .accepted(count(orders, OrderStatus.ACCEPTED))
                .diagnosisInProgress(count(orders, OrderStatus.DIAGNOSIS_IN_PROGRESS))
                .waitingForOwnerApproval(count(orders, OrderStatus.WAITING_FOR_OWNER_APPROVAL))
                .waitingForPart(count(orders, OrderStatus.WAITING_FOR_PART))
                .repairInProgress(count(orders, OrderStatus.REPAIR_IN_PROGRESS))
                .readyForOwner(count(orders, OrderStatus.READY_FOR_OWNER))
                .build();
    }

    private long count(List<Order> orders, OrderStatus status) {
        return orders.stream().filter(order -> order.getStatus() == status).count();
    }
}
