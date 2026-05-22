package com.vladko.autoshopcore.procurement.dto;

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
public class SupplierQuoteResponseDTO {

    private String provider;
    private String sourceCode;
    private String requestedCode;
    private String articleNumber;
    private String brand;
    private String name;
    private String description;
    private Boolean cross;
    private BigDecimal purchasePrice;
    private String currency;
    private String quantityRaw;
    private Integer availableQuantityParsed;
    private Integer minOrderQuantity;
    private Integer deliveryDaysMin;
    private Integer deliveryDaysMax;
    private Integer supplyProbabilityPercent;
    private BigDecimal recommendedSalePrice;
    private BigDecimal marginAmount;
    private Instant fetchedAt;
    private Instant expiresAt;
}
