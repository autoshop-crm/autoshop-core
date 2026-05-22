package com.vladko.autoshopcore.loyalty.service;

import com.vladko.autoshopcore.loyalty.config.CrmLoyaltyProperties;
import com.vladko.autoshopcore.loyalty.dto.LoyaltySettingsResponseDTO;
import com.vladko.autoshopcore.loyalty.exception.InvalidLoyaltyOperationException;
import com.vladko.autoshopcore.order.entity.Order;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

class CrmLoyaltyFacadeTest {

    private CrmLoyaltyProperties props(boolean enabled, boolean earnEnabled, boolean spendEnabled, boolean visible) {
        CrmLoyaltyProperties properties = new CrmLoyaltyProperties();
        properties.setEnabled(enabled);
        properties.setEarnEnabled(earnEnabled);
        properties.setSpendEnabled(spendEnabled);
        properties.setVisible(visible);
        return properties;
    }

    @Test
    void getSettingsShouldReflectDisabledLoyalty() {
        LoyaltyService loyaltyService = mock(LoyaltyService.class);
        CrmLoyaltyFacade facade = new CrmLoyaltyFacadeImpl(loyaltyService, props(false, true, true, true));

        LoyaltySettingsResponseDTO settings = facade.getSettings();

        assertThat(settings.isEnabled()).isFalse();
        assertThat(settings.isVisible()).isFalse();
    }

    @Test
    void applyPointsShouldFailWhenSpendDisabled() {
        LoyaltyService loyaltyService = mock(LoyaltyService.class);
        CrmLoyaltyFacade facade = new CrmLoyaltyFacadeImpl(loyaltyService, props(true, true, false, true));

        assertThatThrownBy(() -> facade.applyPointsToOrder(1, 10))
                .isInstanceOf(InvalidLoyaltyOperationException.class)
                .hasMessage("Loyalty spending is disabled");
    }

    @Test
    void refreshAppliedPointsShouldZeroOutWhenDisabled() {
        LoyaltyService loyaltyService = mock(LoyaltyService.class);
        CrmLoyaltyFacade facade = new CrmLoyaltyFacadeImpl(loyaltyService, props(false, false, false, false));
        Order order = Order.builder().loyaltyPointsSpent(50).pointsDiscountAmount(new BigDecimal("50.00")).build();

        facade.refreshAppliedPointsAfterOrderChange(order);

        assertThat(order.getLoyaltyPointsSpent()).isZero();
        assertThat(order.getPointsDiscountAmount()).isEqualByComparingTo("0.00");
        verifyNoInteractions(loyaltyService);
    }
}
