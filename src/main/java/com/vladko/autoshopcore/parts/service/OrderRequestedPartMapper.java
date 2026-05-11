package com.vladko.autoshopcore.parts.service;

import com.vladko.autoshopcore.parts.dto.OrderRequestedPartResponseDTO;
import com.vladko.autoshopcore.parts.entity.OrderRequestedPart;
import org.springframework.stereotype.Component;

@Component
public class OrderRequestedPartMapper {
    public OrderRequestedPartResponseDTO map(OrderRequestedPart part) {
        return OrderRequestedPartResponseDTO.builder()
                .id(part.getId())
                .orderId(part.getOrder().getId())
                .articleNumber(part.getArticleNumber())
                .brand(part.getBrand())
                .name(part.getName())
                .umapiArticleId(part.getUmapiArticleId())
                .matchedLocalPartId(part.getMatchedLocalPart() == null ? null : part.getMatchedLocalPart().getId())
                .requestedQuantity(part.getRequestedQuantity())
                .status(part.getStatus())
                .selectedSupplier(part.getSelectedSupplier())
                .selectedQuoteSignature(part.getSelectedQuoteSignature())
                .purchasePrice(part.getPurchasePrice())
                .salePrice(part.getSalePrice())
                .currency(part.getCurrency())
                .deliveryDaysMin(part.getDeliveryDaysMin())
                .deliveryDaysMax(part.getDeliveryDaysMax())
                .quoteFetchedAt(part.getQuoteFetchedAt())
                .orderedAt(part.getOrderedAt())
                .receivedAt(part.getReceivedAt())
                .createdAt(part.getCreatedAt())
                .updatedAt(part.getUpdatedAt())
                .build();
    }
}
