package com.vladko.autoshopcore.loyalty.dto;

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
public class LoyaltyAccountResponseDTO {
    private Integer id;
    private Integer customerId;
    private Integer balance;
    private BigDecimal totalSpent;
    private Integer totalEarnedPoints;
    private LoyaltyTierResponseDTO tier;
    private Instant createdAt;
    private Instant updatedAt;
}
