package com.vladko.autoshopcore.order.service;

import com.vladko.autoshopcore.order.dto.OrderResponseDTO;
import com.vladko.autoshopcore.order.entity.LegacyOrderStatus;
import com.vladko.autoshopcore.order.entity.OrderStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LegacyOrderCompatibilityServiceTest {

    @Mock private OrderService orderService;

    private LegacyOrderCompatibilityService service;

    @BeforeEach
    void setUp() {
        service = new LegacyOrderCompatibilityService(orderService, new LegacyOrderStatusProjector());
    }

    @Test
    void shouldExposeLegacyCompatibleStatusAndKeepCrmStatus() {
        when(orderService.getById(5)).thenReturn(OrderResponseDTO.builder()
                .id(5)
                .status(OrderStatus.WAITING_FOR_PART)
                .build());

        OrderResponseDTO response = service.getById(5);

        assertThat(response.getStatus()).isEqualTo(OrderStatus.IN_PROGRESS);
        assertThat(response.getCrmStatus()).isEqualTo(OrderStatus.WAITING_FOR_PART);
        assertThat(response.getLegacyStatus()).isEqualTo(LegacyOrderStatus.IN_PROGRESS);
    }

    @Test
    void shouldFilterByProjectedLegacyStatus() {
        when(orderService.getAll()).thenReturn(List.of(
                OrderResponseDTO.builder().id(1).status(OrderStatus.ACCEPTED).build(),
                OrderResponseDTO.builder().id(2).status(OrderStatus.HANDED_OVER).build()
        ));

        List<OrderResponseDTO> response = service.getAllByStatus(LegacyOrderStatus.NEW);

        assertThat(response).hasSize(1);
        assertThat(response.get(0).getId()).isEqualTo(1);
        assertThat(response.get(0).getCrmStatus()).isEqualTo(OrderStatus.ACCEPTED);
    }
}
