package com.vladko.autoshopcore.parts.service;

import com.vladko.autoshopcore.order.entity.Order;
import com.vladko.autoshopcore.order.exception.InvalidOrderStateException;
import com.vladko.autoshopcore.order.repository.OrderRepository;
import com.vladko.autoshopcore.order.service.OrderFinancialsService;
import com.vladko.autoshopcore.parts.dto.OrderRequestedPartReceiveDTO;
import com.vladko.autoshopcore.parts.dto.OrderRequestedPartResponseDTO;
import com.vladko.autoshopcore.parts.entity.OrderRequestedPart;
import com.vladko.autoshopcore.parts.entity.OrderRequestedPartStatus;
import com.vladko.autoshopcore.parts.entity.Part;
import com.vladko.autoshopcore.parts.exception.OrderRequestedPartNotFoundException;
import com.vladko.autoshopcore.parts.exception.PartNotFoundException;
import com.vladko.autoshopcore.parts.repository.OrderRequestedPartRepository;
import com.vladko.autoshopcore.parts.repository.PartRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Locale;

@Service
@RequiredArgsConstructor
public class OrderRequestedPartReceiptServiceImpl implements OrderRequestedPartReceiptService {

    private final OrderRequestedPartRepository orderRequestedPartRepository;
    private final PartRepository partRepository;
    private final OrderRepository orderRepository;
    private final OrderFinancialsService orderFinancialsService;
    private final OrderRequestedPartMapper mapper;

    @Override
    @Transactional
    public OrderRequestedPartResponseDTO receive(Integer orderId, Integer requestedPartId, OrderRequestedPartReceiveDTO dto) {
        OrderRequestedPart requestedPart = orderRequestedPartRepository.findByIdAndOrderId(requestedPartId, orderId)
                .orElseThrow(() -> new OrderRequestedPartNotFoundException(orderId, requestedPartId));
        Order order = requestedPart.getOrder();
        ensureEditable(order);
        if (requestedPart.getStatus() != OrderRequestedPartStatus.ORDERED_IN_TRANSIT) {
            throw new InvalidOrderStateException("Requested part must be ORDERED_IN_TRANSIT before receipt");
        }
        if (dto.getReceivedQuantity() < requestedPart.getRequestedQuantity()) {
            throw new IllegalArgumentException("Received quantity cannot be lower than requested quantity in first version");
        }

        Part part = resolveTargetPart(requestedPart, dto);
        part.setStockQuantity(part.getStockQuantity() + dto.getReceivedQuantity());
        if (dto.getSalePrice() != null) {
            part.setCost(dto.getSalePrice());
        } else if (requestedPart.getSalePrice() != null) {
            part.setCost(requestedPart.getSalePrice());
        }
        part.setReservedQuantity(part.getReservedQuantity() + requestedPart.getRequestedQuantity());
        Part savedPart = partRepository.save(part);

        requestedPart.setMatchedLocalPart(savedPart);
        requestedPart.setStatus(OrderRequestedPartStatus.IN_STOCK_RESERVED);
        requestedPart.setReceivedAt(Instant.now());
        orderRequestedPartRepository.save(requestedPart);

        orderFinancialsService.recalculateAfterMutableTotalsChange(order);
        orderRepository.save(order);
        return mapper.map(requestedPart);
    }

    private Part resolveTargetPart(OrderRequestedPart requestedPart, OrderRequestedPartReceiveDTO dto) {
        if (dto.getTargetPartId() != null) {
            return partRepository.findById(dto.getTargetPartId()).orElseThrow(() -> new PartNotFoundException(dto.getTargetPartId()));
        }
        if (requestedPart.getMatchedLocalPart() != null) {
            return partRepository.findById(requestedPart.getMatchedLocalPart().getId()).orElseThrow(() -> new PartNotFoundException(requestedPart.getMatchedLocalPart().getId()));
        }
        return Part.builder()
                .articleNumber(requestedPart.getArticleNumber().trim().toUpperCase(Locale.ROOT))
                .brand(dto.getBrand() != null && !dto.getBrand().isBlank() ? dto.getBrand().trim() : requestedPart.getBrand())
                .name(dto.getName() != null && !dto.getName().isBlank() ? dto.getName().trim() : requestedPart.getName())
                .cost(requestedPart.getSalePrice())
                .stockQuantity(0)
                .reservedQuantity(0)
                .build();
    }

    private void ensureEditable(Order order) {
        switch (order.getStatus()) {
            case NEW, IN_PROGRESS -> { }
            default -> throw new InvalidOrderStateException("Order in status '%s' can no longer be updated".formatted(order.getStatus()));
        }
    }
}
