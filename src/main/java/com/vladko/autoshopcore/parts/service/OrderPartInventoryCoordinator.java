package com.vladko.autoshopcore.parts.service;

import com.vladko.autoshopcore.order.entity.Order;
import com.vladko.autoshopcore.order.entity.OrderStatus;
import com.vladko.autoshopcore.order.exception.InvalidOrderStateException;
import com.vladko.autoshopcore.order.exception.OrderConflictException;
import com.vladko.autoshopcore.order.exception.OrderNotFoundException;
import com.vladko.autoshopcore.order.repository.OrderRepository;
import com.vladko.autoshopcore.order.service.OrderFinancialsService;
import com.vladko.autoshopcore.loyalty.service.LoyaltyService;
import com.vladko.autoshopcore.parts.dto.OrderPartItemCreateDTO;
import com.vladko.autoshopcore.parts.dto.OrderPartItemResponseDTO;
import com.vladko.autoshopcore.parts.dto.OrderPartItemUpdateDTO;
import com.vladko.autoshopcore.parts.entity.OrderPartItem;
import com.vladko.autoshopcore.parts.entity.Part;
import com.vladko.autoshopcore.parts.exception.InsufficientPartStockException;
import com.vladko.autoshopcore.parts.exception.OrderPartItemNotFoundException;
import com.vladko.autoshopcore.parts.exception.PartNotFoundException;
import com.vladko.autoshopcore.parts.repository.OrderPartItemRepository;
import com.vladko.autoshopcore.parts.repository.PartRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Service
@RequiredArgsConstructor
public class OrderPartInventoryCoordinator {

    private final OrderRepository orderRepository;
    private final PartRepository partRepository;
    private final OrderPartItemRepository orderPartItemRepository;
    private final OrderFinancialsService orderFinancialsService;
    private final LoyaltyService loyaltyService;

    @Transactional
    public OrderPartItemResponseDTO create(Integer orderId, OrderPartItemCreateDTO dto) {
        Order order = findOrder(orderId);
        ensureOrderPartsEditable(order);

        if (orderPartItemRepository.findByOrderIdAndPartId(orderId, dto.getPartId()).isPresent()) {
            throw new OrderConflictException("Part is already reserved for this order");
        }

        Part part = findPartForUpdate(dto.getPartId());
        reserveQuantity(part, dto.getQuantity());

        OrderPartItem item = OrderPartItem.builder()
                .order(order)
                .part(part)
                .quantity(dto.getQuantity())
                .unitPrice(part.getCost())
                .build();

        OrderPartItem savedItem = orderPartItemRepository.save(item);
        partRepository.save(part);
        orderPartItemRepository.flush();

        orderFinancialsService.recalculateAfterMutableTotalsChange(order);
        loyaltyService.refreshAppliedPointsAfterOrderChange(order);
        orderRepository.save(order);

        return mapToResponse(savedItem);
    }

    @Transactional(readOnly = true)
    public List<OrderPartItemResponseDTO> getAllByOrderId(Integer orderId) {
        findOrder(orderId);
        return orderPartItemRepository.findAllByOrderIdOrderByIdAsc(orderId)
                .stream()
                .map(this::mapToResponse)
                .toList();
    }

    @Transactional
    public OrderPartItemResponseDTO update(Integer orderId, Integer itemId, OrderPartItemUpdateDTO dto) {
        Order order = findOrder(orderId);
        ensureOrderPartsEditable(order);

        OrderPartItem item = orderPartItemRepository.findByIdAndOrderId(itemId, orderId)
                .orElseThrow(() -> new OrderPartItemNotFoundException(orderId, itemId));

        Part part = findPartForUpdate(item.getPart().getId());
        int delta = dto.getQuantity() - item.getQuantity();
        adjustReservedQuantity(part, delta);

        item.setQuantity(dto.getQuantity());
        partRepository.save(part);
        orderPartItemRepository.save(item);
        orderPartItemRepository.flush();

        orderFinancialsService.recalculateAfterMutableTotalsChange(order);
        loyaltyService.refreshAppliedPointsAfterOrderChange(order);
        orderRepository.save(order);

        return mapToResponse(item);
    }

    @Transactional
    public void delete(Integer orderId, Integer itemId) {
        Order order = findOrder(orderId);
        ensureOrderPartsEditable(order);

        OrderPartItem item = orderPartItemRepository.findByIdAndOrderId(itemId, orderId)
                .orElseThrow(() -> new OrderPartItemNotFoundException(orderId, itemId));

        Part part = findPartForUpdate(item.getPart().getId());
        releaseQuantity(part, item.getQuantity());

        orderPartItemRepository.delete(item);
        orderPartItemRepository.flush();
        partRepository.save(part);

        orderFinancialsService.recalculateAfterMutableTotalsChange(order);
        loyaltyService.refreshAppliedPointsAfterOrderChange(order);
        orderRepository.save(order);
    }

    @Transactional
    public void finalizeReservations(Order order) {
        List<OrderPartItem> items = orderPartItemRepository.findAllByOrderIdOrderByIdAsc(order.getId());
        for (OrderPartItem item : items) {
            Part part = findPartForUpdate(item.getPart().getId());
            part.setStockQuantity(part.getStockQuantity() - item.getQuantity());
            part.setReservedQuantity(part.getReservedQuantity() - item.getQuantity());
            partRepository.save(part);
        }
    }

    @Transactional
    public void releaseReservations(Order order) {
        List<OrderPartItem> items = orderPartItemRepository.findAllByOrderIdOrderByIdAsc(order.getId());
        for (OrderPartItem item : items) {
            Part part = findPartForUpdate(item.getPart().getId());
            releaseQuantity(part, item.getQuantity());
            partRepository.save(part);
        }
    }

    private Order findOrder(Integer orderId) {
        return orderRepository.findById(orderId)
                .orElseThrow(() -> new OrderNotFoundException(orderId));
    }

    private Part findPartForUpdate(Integer partId) {
        return partRepository.findByIdForUpdate(partId)
                .orElseThrow(() -> new PartNotFoundException(partId));
    }

    private void ensureOrderPartsEditable(Order order) {
        if (order.getStatus() != OrderStatus.NEW && order.getStatus() != OrderStatus.IN_PROGRESS) {
            throw new InvalidOrderStateException(
                    "Order in status '%s' can no longer be updated".formatted(order.getStatus())
            );
        }
    }

    private void reserveQuantity(Part part, int quantity) {
        if (quantity <= 0) {
            throw new IllegalArgumentException("Quantity must be greater than zero");
        }

        int availableQuantity = part.getStockQuantity() - part.getReservedQuantity();
        if (availableQuantity < quantity) {
            throw new InsufficientPartStockException(
                    "Part with id '%s' does not have enough available stock".formatted(part.getId())
            );
        }

        part.setReservedQuantity(part.getReservedQuantity() + quantity);
    }

    private void adjustReservedQuantity(Part part, int delta) {
        if (delta > 0) {
            reserveQuantity(part, delta);
            return;
        }

        if (delta < 0) {
            releaseQuantity(part, Math.abs(delta));
        }
    }

    private void releaseQuantity(Part part, int quantity) {
        if (quantity <= 0) {
            throw new IllegalArgumentException("Quantity must be greater than zero");
        }

        if (part.getReservedQuantity() < quantity) {
            throw new OrderConflictException(
                    "Part with id '%s' does not have enough reserved quantity".formatted(part.getId())
            );
        }

        part.setReservedQuantity(part.getReservedQuantity() - quantity);
    }

    private OrderPartItemResponseDTO mapToResponse(OrderPartItem item) {
        return OrderPartItemResponseDTO.builder()
                .id(item.getId())
                .orderId(item.getOrder().getId())
                .partId(item.getPart().getId())
                .articleNumber(item.getPart().getArticleNumber())
                .brand(item.getPart().getBrand())
                .name(item.getPart().getName())
                .quantity(item.getQuantity())
                .unitPrice(item.getUnitPrice())
                .lineTotal(item.getUnitPrice().multiply(BigDecimal.valueOf(item.getQuantity())))
                .build();
    }
}
