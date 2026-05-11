package com.vladko.autoshopcore.parts.service;

import com.vladko.autoshopcore.order.exception.OrderNotFoundException;
import com.vladko.autoshopcore.order.repository.OrderRepository;
import com.vladko.autoshopcore.parts.dto.OrderPartOverviewItemDTO;
import com.vladko.autoshopcore.parts.dto.OrderPartsOverviewResponseDTO;
import com.vladko.autoshopcore.parts.entity.OrderRequestedPart;
import com.vladko.autoshopcore.parts.repository.OrderPartItemRepository;
import com.vladko.autoshopcore.parts.repository.OrderRequestedPartRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class OrderPartsOverviewServiceImpl implements OrderPartsOverviewService {

    private final OrderRepository orderRepository;
    private final OrderPartItemRepository orderPartItemRepository;
    private final OrderRequestedPartRepository orderRequestedPartRepository;

    @Override
    @Transactional(readOnly = true)
    public OrderPartsOverviewResponseDTO getOverview(Integer orderId) {
        orderRepository.findById(orderId).orElseThrow(() -> new OrderNotFoundException(orderId));
        List<OrderPartOverviewItemDTO> items = new ArrayList<>();
        orderPartItemRepository.findAllByOrderIdOrderByIdAsc(orderId).forEach(item -> items.add(OrderPartOverviewItemDTO.builder()
                .itemType("LOCAL")
                .id(item.getId())
                .orderId(orderId)
                .localPartId(item.getPart().getId())
                .articleNumber(item.getPart().getArticleNumber())
                .brand(item.getPart().getBrand())
                .name(item.getPart().getName())
                .quantity(item.getQuantity())
                .unitPrice(item.getUnitPrice())
                .lineTotal(item.getUnitPrice().multiply(BigDecimal.valueOf(item.getQuantity())))
                .availableLocally(true)
                .build()));
        for (OrderRequestedPart requestedPart : orderRequestedPartRepository.findAllByOrderIdOrderByIdAsc(orderId)) {
            BigDecimal unitPrice = requestedPart.getSalePrice();
            items.add(OrderPartOverviewItemDTO.builder()
                    .itemType("REQUESTED")
                    .id(requestedPart.getId())
                    .orderId(orderId)
                    .localPartId(requestedPart.getMatchedLocalPart() == null ? null : requestedPart.getMatchedLocalPart().getId())
                    .articleNumber(requestedPart.getArticleNumber())
                    .brand(requestedPart.getBrand())
                    .name(requestedPart.getName())
                    .quantity(requestedPart.getRequestedQuantity())
                    .requestedStatus(requestedPart.getStatus())
                    .unitPrice(unitPrice)
                    .lineTotal(unitPrice == null ? null : unitPrice.multiply(BigDecimal.valueOf(requestedPart.getRequestedQuantity())))
                    .availableLocally(requestedPart.getStatus() == com.vladko.autoshopcore.parts.entity.OrderRequestedPartStatus.IN_STOCK_RESERVED)
                    .build());
        }
        return OrderPartsOverviewResponseDTO.builder().orderId(orderId).items(items).build();
    }
}
