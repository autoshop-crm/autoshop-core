package com.vladko.autoshopcore.parts.dto;

import com.vladko.autoshopcore.parts.entity.OrderRequestedPartStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderPartOverviewItemDTO {
    private String itemType;
    private Integer id;
    private Integer orderId;
    private Integer localPartId;
    private String articleNumber;
    private String brand;
    private String name;
    private Integer quantity;
    private OrderRequestedPartStatus requestedStatus;
    private BigDecimal unitPrice;
    private BigDecimal lineTotal;
    private boolean availableLocally;
}
