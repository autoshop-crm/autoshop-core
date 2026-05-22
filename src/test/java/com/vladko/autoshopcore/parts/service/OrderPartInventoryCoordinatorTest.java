package com.vladko.autoshopcore.parts.service;

import com.vladko.autoshopcore.order.entity.Order;
import com.vladko.autoshopcore.order.entity.OrderStatus;
import com.vladko.autoshopcore.order.exception.OrderConflictException;
import com.vladko.autoshopcore.order.repository.OrderRepository;
import com.vladko.autoshopcore.order.service.OrderFinancialsService;
import com.vladko.autoshopcore.loyalty.service.LoyaltyService;
import com.vladko.autoshopcore.parts.dto.OrderPartItemCreateDTO;
import com.vladko.autoshopcore.parts.dto.OrderPartItemResponseDTO;
import com.vladko.autoshopcore.parts.dto.OrderPartItemUpdateDTO;
import com.vladko.autoshopcore.parts.entity.OrderPartItem;
import com.vladko.autoshopcore.parts.entity.Part;
import com.vladko.autoshopcore.parts.exception.InsufficientPartStockException;
import com.vladko.autoshopcore.parts.repository.OrderPartItemRepository;
import com.vladko.autoshopcore.parts.repository.PartRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OrderPartInventoryCoordinatorTest {

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private PartRepository partRepository;

    @Mock
    private OrderPartItemRepository orderPartItemRepository;

    @Mock
    private OrderFinancialsService orderFinancialsService;

    @Mock
    private LoyaltyService loyaltyService;

    @InjectMocks
    private OrderPartInventoryCoordinator coordinator;

    @Test
    void createShouldReservePartAndFreezeUnitPrice() {
        Order order = Order.builder().id(10).status(OrderStatus.NEW).build();
        Part part = Part.builder()
                .id(5)
                .name("Oil filter")
                .articleNumber("OF-123")
                .brand("Bosch")
                .cost(new BigDecimal("15.50"))
                .stockQuantity(8)
                .reservedQuantity(2)
                .build();

        when(orderRepository.findById(10)).thenReturn(Optional.of(order));
        when(orderPartItemRepository.findByOrderIdAndPartId(10, 5)).thenReturn(Optional.empty());
        when(partRepository.findByIdForUpdate(5)).thenReturn(Optional.of(part));
        when(orderPartItemRepository.save(any(OrderPartItem.class))).thenAnswer(invocation -> {
            OrderPartItem item = invocation.getArgument(0);
            item.setId(77);
            return item;
        });
        doNothing().when(orderFinancialsService).recalculateAfterMutableTotalsChange(order);

        OrderPartItemResponseDTO response = coordinator.create(10, new OrderPartItemCreateDTO(5, 3));

        assertThat(part.getReservedQuantity()).isEqualTo(5);
        assertThat(response.getId()).isEqualTo(77);
        assertThat(response.getUnitPrice()).isEqualByComparingTo("15.50");
        assertThat(response.getLineTotal()).isEqualByComparingTo("46.50");
        verify(orderFinancialsService).recalculateAfterMutableTotalsChange(order);
        verify(loyaltyService).refreshAppliedPointsAfterOrderChange(order);
    }

    @Test
    void createShouldRejectDuplicatePartInOrder() {
        Order order = Order.builder().id(10).status(OrderStatus.NEW).build();

        when(orderRepository.findById(10)).thenReturn(Optional.of(order));
        when(orderPartItemRepository.findByOrderIdAndPartId(10, 5))
                .thenReturn(Optional.of(OrderPartItem.builder().id(88).build()));

        assertThatThrownBy(() -> coordinator.create(10, new OrderPartItemCreateDTO(5, 1)))
                .isInstanceOf(OrderConflictException.class)
                .hasMessage("Part is already reserved for this order");
    }

    @Test
    void createShouldRejectInsufficientAvailableStock() {
        Order order = Order.builder().id(10).status(OrderStatus.NEW).build();
        Part part = Part.builder()
                .id(5)
                .name("Oil filter")
                .articleNumber("OF-123")
                .cost(new BigDecimal("15.50"))
                .stockQuantity(4)
                .reservedQuantity(3)
                .build();

        when(orderRepository.findById(10)).thenReturn(Optional.of(order));
        when(orderPartItemRepository.findByOrderIdAndPartId(10, 5)).thenReturn(Optional.empty());
        when(partRepository.findByIdForUpdate(5)).thenReturn(Optional.of(part));

        assertThatThrownBy(() -> coordinator.create(10, new OrderPartItemCreateDTO(5, 2)))
                .isInstanceOf(InsufficientPartStockException.class)
                .hasMessage("Part with id '5' does not have enough available stock");
    }

    @Test
    void updateShouldAdjustReservedQuantityByDelta() {
        Order order = Order.builder().id(10).status(OrderStatus.NEW).build();
        Part part = Part.builder()
                .id(5)
                .name("Oil filter")
                .articleNumber("OF-123")
                .cost(new BigDecimal("15.50"))
                .stockQuantity(10)
                .reservedQuantity(4)
                .build();
        OrderPartItem item = OrderPartItem.builder()
                .id(77)
                .order(order)
                .part(part)
                .quantity(3)
                .unitPrice(new BigDecimal("15.50"))
                .build();

        when(orderRepository.findById(10)).thenReturn(Optional.of(order));
        when(orderPartItemRepository.findByIdAndOrderId(77, 10)).thenReturn(Optional.of(item));
        when(partRepository.findByIdForUpdate(5)).thenReturn(Optional.of(part));

        OrderPartItemResponseDTO response = coordinator.update(10, 77, new OrderPartItemUpdateDTO(1));

        assertThat(part.getReservedQuantity()).isEqualTo(2);
        assertThat(item.getQuantity()).isEqualTo(1);
        assertThat(response.getLineTotal()).isEqualByComparingTo("15.50");
        verify(orderFinancialsService).recalculateAfterMutableTotalsChange(order);
        verify(loyaltyService).refreshAppliedPointsAfterOrderChange(order);
    }

    @Test
    void deleteShouldReleaseReservedQuantity() {
        Order order = Order.builder().id(10).status(OrderStatus.NEW).build();
        Part part = Part.builder()
                .id(5)
                .name("Oil filter")
                .articleNumber("OF-123")
                .cost(new BigDecimal("15.50"))
                .stockQuantity(10)
                .reservedQuantity(4)
                .build();
        OrderPartItem item = OrderPartItem.builder()
                .id(77)
                .order(order)
                .part(part)
                .quantity(3)
                .unitPrice(new BigDecimal("15.50"))
                .build();

        when(orderRepository.findById(10)).thenReturn(Optional.of(order));
        when(orderPartItemRepository.findByIdAndOrderId(77, 10)).thenReturn(Optional.of(item));
        when(partRepository.findByIdForUpdate(5)).thenReturn(Optional.of(part));

        coordinator.delete(10, 77);

        assertThat(part.getReservedQuantity()).isEqualTo(1);
        verify(orderPartItemRepository).delete(item);
        verify(orderFinancialsService).recalculateAfterMutableTotalsChange(order);
        verify(loyaltyService).refreshAppliedPointsAfterOrderChange(order);
    }
}
