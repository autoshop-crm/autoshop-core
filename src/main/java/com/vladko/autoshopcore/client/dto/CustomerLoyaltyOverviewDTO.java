package com.vladko.autoshopcore.client.dto;

import com.vladko.autoshopcore.loyalty.dto.LoyaltyAccountResponseDTO;
import com.vladko.autoshopcore.loyalty.dto.LoyaltyTierResponseDTO;
import com.vladko.autoshopcore.loyalty.dto.LoyaltyTransactionResponseDTO;
import lombok.Builder;
import lombok.Value;

import java.util.List;

@Value
@Builder
public class CustomerLoyaltyOverviewDTO {
    LoyaltyAccountResponseDTO account;
    List<LoyaltyTransactionResponseDTO> recentTransactions;
    List<LoyaltyTierResponseDTO> tiers;
}
