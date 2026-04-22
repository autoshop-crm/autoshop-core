package com.vladko.autoshopcore.loyalty.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LoyaltyTierResponseDTO {
    private Integer id;
    private String name;
    private BigDecimal entrySpentMoney;
    private Integer discountPercent;
    private Integer maxPointsPaymentPercent;
}
