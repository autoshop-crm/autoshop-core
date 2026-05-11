package com.vladko.autoshopcore.loyalty.service;

import com.vladko.autoshopcore.loyalty.dto.LoyaltyAccountResponseDTO;
import com.vladko.autoshopcore.loyalty.dto.LoyaltyTierResponseDTO;
import com.vladko.autoshopcore.loyalty.dto.LoyaltyTransactionResponseDTO;
import com.vladko.autoshopcore.order.entity.Order;

import java.util.List;

public interface LoyaltyService {

    LoyaltyAccountResponseDTO getOrCreateAccountByCustomerId(Integer customerId);

    List<LoyaltyTierResponseDTO> getTiers();

    List<LoyaltyTransactionResponseDTO> getTransactions(Integer accountId);

    void applyPointsToOrder(Integer orderId, Integer points);

    void removePointsFromOrder(Integer orderId);

    void refreshAppliedPointsAfterOrderChange(Order order);

    Integer processOrderCompleted(Order order);

    void processOrderCancelled(Order order);
}
