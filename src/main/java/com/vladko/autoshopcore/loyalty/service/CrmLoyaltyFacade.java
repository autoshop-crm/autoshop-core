package com.vladko.autoshopcore.loyalty.service;

import com.vladko.autoshopcore.loyalty.dto.LoyaltyAccountResponseDTO;
import com.vladko.autoshopcore.loyalty.dto.LoyaltySettingsResponseDTO;
import com.vladko.autoshopcore.order.entity.Order;

public interface CrmLoyaltyFacade {
    LoyaltySettingsResponseDTO getSettings();
    LoyaltyAccountResponseDTO getVisibleAccountByCustomerId(Integer customerId);
    void applyPointsToOrder(Integer orderId, Integer points);
    void removePointsFromOrder(Integer orderId);
    void refreshAppliedPointsAfterOrderChange(Order order);
    Integer processOrderCompleted(Order order);
    void processOrderCancelled(Order order);
}
