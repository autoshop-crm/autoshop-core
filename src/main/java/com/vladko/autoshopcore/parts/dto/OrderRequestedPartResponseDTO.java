package com.vladko.autoshopcore.parts.dto;

import com.vladko.autoshopcore.parts.entity.OrderRequestedPartStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderRequestedPartResponseDTO {
    private Integer id;
    private Integer orderId;
    private String articleNumber;
    private String brand;
    private String name;
    private Integer umapiArticleId;
    private Integer matchedLocalPartId;
    private Integer requestedQuantity;
    private OrderRequestedPartStatus status;
    private String selectedSupplier;
    private String selectedQuoteSignature;
    private BigDecimal purchasePrice;
    private BigDecimal salePrice;
    private String currency;
    private Integer deliveryDaysMin;
    private Integer deliveryDaysMax;
    private Instant quoteFetchedAt;
    private Instant orderedAt;
    private Instant receivedAt;
    private Instant createdAt;
    private Instant updatedAt;
}
