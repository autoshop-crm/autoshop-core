package com.vladko.autoshopcore.order.query.service;

import com.vladko.autoshopcore.loyalty.dto.LoyaltySettingsResponseDTO;
import com.vladko.autoshopcore.loyalty.service.CrmLoyaltyFacade;
import com.vladko.autoshopcore.order.dto.OrderResponseDTO;
import com.vladko.autoshopcore.order.entity.Order;
import com.vladko.autoshopcore.order.entity.OrderStatus;
import com.vladko.autoshopcore.order.repository.OrderRepository;
import com.vladko.autoshopcore.order.service.OrderService;
import com.vladko.autoshopcore.security.CoreSecurityService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OrderQueryServiceTest {

    @Mock private OrderRepository orderRepository;
    @Mock private OrderService orderService;
    @Mock private CoreSecurityService coreSecurityService;
    @Mock private CrmLoyaltyFacade crmLoyaltyFacade;

    private OrderQueryService service;

    @BeforeEach
    void setUp() {
        service = new OrderQueryServiceImpl(orderRepository, orderService, coreSecurityService, crmLoyaltyFacade);
    }

    @Test
    void queueSummaryShouldCountOperationalStatuses() {
        when(orderRepository.findAll()).thenReturn(List.of(
                Order.builder().status(OrderStatus.WAITING_FOR_PART).build(),
                Order.builder().status(OrderStatus.WAITING_FOR_PART).build(),
                Order.builder().status(OrderStatus.READY_FOR_OWNER).build()
        ));

        var summary = service.queueSummary();

        assertThat(summary.getWaitingForPart()).isEqualTo(2);
        assertThat(summary.getReadyForOwner()).isEqualTo(1);
    }


    @Test
    void searchShouldNormalizeQueryBeforeRepositoryCall() {
        when(orderRepository.searchForCrm(null, null, null, null, null, null, "engine")).thenReturn(List.of());
        when(crmLoyaltyFacade.getSettings()).thenReturn(LoyaltySettingsResponseDTO.builder().enabled(true).earnEnabled(true).spendEnabled(true).visible(true).build());

        var response = service.search(null, null, null, null, null, null, "  EnGinE  ", 0, 10);

        assertThat(response.getItems()).isEmpty();
        verify(orderRepository).searchForCrm(null, null, null, null, null, null, "engine");
    }

    @Test
    void searchShouldReturnPagedItemsAndLoyaltySettings() {
        when(orderRepository.searchForCrm(null, null, null, null, null, null, null)).thenReturn(List.of(
                Order.builder().id(1).build(),
                Order.builder().id(2).build()
        ));
        when(orderService.getById(1)).thenReturn(OrderResponseDTO.builder().id(1).build());
        when(crmLoyaltyFacade.getSettings()).thenReturn(LoyaltySettingsResponseDTO.builder().enabled(true).earnEnabled(true).spendEnabled(true).visible(true).build());

        var response = service.search(null, null, null, null, null, null, null, 0, 1);

        assertThat(response.getItems()).hasSize(1);
        assertThat(response.isHasMore()).isTrue();
        assertThat(response.getLoyaltySettings().isEnabled()).isTrue();
    }
}
