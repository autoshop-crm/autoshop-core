package com.vladko.autoshopcore.procurement.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PurchaseOrderResponseDTO {

    private String provider;
    private Integer externalOrderId;
    private String externalOrderNumber;
    private Integer externalStatus;
    private String externalStatusDisplay;
    private String articleNumber;
    private String brand;
    private String name;
    private Integer quantity;
    private BigDecimal purchaseUnitPrice;
    private BigDecimal saleUnitPrice;
    private BigDecimal purchaseTotal;
    private BigDecimal saleTotal;
    private boolean externalOrderCreated;
    private boolean testMode;
}
