package com.vladko.autoshopcore.loyalty.service;

import com.vladko.autoshopcore.loyalty.config.CrmLoyaltyProperties;
import com.vladko.autoshopcore.loyalty.dto.LoyaltyAccountResponseDTO;
import com.vladko.autoshopcore.loyalty.dto.LoyaltySettingsResponseDTO;
import com.vladko.autoshopcore.loyalty.exception.InvalidLoyaltyOperationException;
import com.vladko.autoshopcore.order.entity.Order;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class CrmLoyaltyFacadeImpl implements CrmLoyaltyFacade {

    private final LoyaltyService loyaltyService;
    private final CrmLoyaltyProperties properties;

    @Override
    public LoyaltySettingsResponseDTO getSettings() {
        return LoyaltySettingsResponseDTO.builder()
                .enabled(properties.enabled())
                .earnEnabled(properties.enabled() && properties.earnEnabled())
                .spendEnabled(properties.enabled() && properties.spendEnabled())
                .visible(properties.enabled() && properties.visible())
                .build();
    }

    @Override
    public LoyaltyAccountResponseDTO getVisibleAccountByCustomerId(Integer customerId) {
        if (!properties.enabled() || !properties.visible()) {
            return null;
        }
        return loyaltyService.getOrCreateAccountByCustomerId(customerId);
    }

    @Override
    public void applyPointsToOrder(Integer orderId, Integer points) {
        ensureSpendEnabled();
        loyaltyService.applyPointsToOrder(orderId, points);
    }

    @Override
    public void removePointsFromOrder(Integer orderId) {
        ensureSpendEnabled();
        loyaltyService.removePointsFromOrder(orderId);
    }

    @Override
    public void refreshAppliedPointsAfterOrderChange(Order order) {
        if (!properties.enabled()) {
            order.setPointsDiscountAmount(java.math.BigDecimal.ZERO);
            order.setLoyaltyPointsSpent(0);
            return;
        }
        loyaltyService.refreshAppliedPointsAfterOrderChange(order);
    }

    @Override
    public Integer processOrderCompleted(Order order) {
        if (!properties.enabled() || !properties.earnEnabled()) {
            return 0;
        }
        return loyaltyService.processOrderCompleted(order);
    }

    @Override
    public void processOrderCancelled(Order order) {
        if (!properties.enabled()) {
            order.setPointsDiscountAmount(java.math.BigDecimal.ZERO);
            order.setLoyaltyPointsSpent(0);
            return;
        }
        loyaltyService.processOrderCancelled(order);
    }

    private void ensureSpendEnabled() {
        if (!properties.enabled() || !properties.spendEnabled()) {
            throw new InvalidLoyaltyOperationException("Loyalty spending is disabled");
        }
    }
}
