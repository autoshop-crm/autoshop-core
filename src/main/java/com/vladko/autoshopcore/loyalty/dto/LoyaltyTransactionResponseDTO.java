package com.vladko.autoshopcore.loyalty.dto;

import com.vladko.autoshopcore.loyalty.entity.LoyaltyTransactionReason;
import com.vladko.autoshopcore.loyalty.entity.OperationType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LoyaltyTransactionResponseDTO {
    private Integer id;
    private Integer accountId;
    private Integer orderId;
    private OperationType operationType;
    private LoyaltyTransactionReason reason;
    private Integer pointsAmount;
    private Instant createdAt;
}
