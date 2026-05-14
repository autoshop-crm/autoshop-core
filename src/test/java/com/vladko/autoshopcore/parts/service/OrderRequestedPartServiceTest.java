package com.vladko.autoshopcore.parts.service;

import com.vladko.autoshopcore.order.entity.Order;
import com.vladko.autoshopcore.order.entity.OrderStatus;
import com.vladko.autoshopcore.order.repository.OrderRepository;
import com.vladko.autoshopcore.order.service.OrderFinancialsService;
import com.vladko.autoshopcore.order.timeline.service.OrderTimelineService;
import com.vladko.autoshopcore.security.CoreSecurityService;
import com.vladko.autoshopcore.parts.dto.OrderRequestedPartCreateDTO;
import com.vladko.autoshopcore.parts.dto.OrderRequestedPartOrderDTO;
import com.vladko.autoshopcore.parts.dto.OrderRequestedPartReceiveDTO;
import com.vladko.autoshopcore.parts.dto.OrderRequestedPartResponseDTO;
import com.vladko.autoshopcore.parts.entity.OrderRequestedPart;
import com.vladko.autoshopcore.parts.entity.OrderRequestedPartStatus;
import com.vladko.autoshopcore.parts.entity.Part;
import com.vladko.autoshopcore.parts.repository.OrderRequestedPartRepository;
import com.vladko.autoshopcore.parts.repository.PartRepository;
import com.vladko.autoshopcore.procurement.dto.CarretaQuoteOrderDTO;
import com.vladko.autoshopcore.procurement.service.PurchaseOrderService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OrderRequestedPartServiceTest {

    @Mock private OrderRepository orderRepository;
    @Mock private PartRepository partRepository;
    @Mock private OrderRequestedPartRepository orderRequestedPartRepository;
    @Mock private PurchaseOrderService purchaseOrderService;
    @Mock private OrderFinancialsService orderFinancialsService;
    @Mock private CoreSecurityService coreSecurityService;
    @Mock private OrderTimelineService orderTimelineService;

    private OrderRequestedPartMapper mapper;
    private OrderRequestedPartService service;
    private OrderRequestedPartProcurementService procurementService;
    private OrderRequestedPartReceiptService receiptService;

    @BeforeEach
    void setUp() {
        mapper = new OrderRequestedPartMapper();
        service = new OrderRequestedPartServiceImpl(orderRepository, partRepository, orderRequestedPartRepository, mapper);
        procurementService = new OrderRequestedPartProcurementServiceImpl(orderRequestedPartRepository, purchaseOrderService, orderRepository, orderFinancialsService, mapper, coreSecurityService, orderTimelineService);
        receiptService = new OrderRequestedPartReceiptServiceImpl(orderRequestedPartRepository, partRepository, orderRepository, orderFinancialsService, mapper, coreSecurityService, orderTimelineService);
        lenient().when(coreSecurityService.currentActor()).thenReturn(new com.vladko.autoshopcore.security.CoreActor(1L, com.vladko.autoshopcore.order.timeline.entity.OrderTimelineActorType.MANAGER));
        lenient().when(coreSecurityService.requireRoles(any())).thenReturn(new com.vladko.autoshopcore.security.CoreActor(1L, com.vladko.autoshopcore.order.timeline.entity.OrderTimelineActorType.MANAGER));
    }

    @Test
    void createShouldPersistRequestedPartWithOutOfStockStatus() {
        Order order = Order.builder().id(3).status(OrderStatus.NEW).build();
        when(orderRepository.findById(3)).thenReturn(Optional.of(order));
        when(orderRequestedPartRepository.save(any(OrderRequestedPart.class))).thenAnswer(inv -> {
            OrderRequestedPart part = inv.getArgument(0);
            part.setId(11);
            return part;
        });

        OrderRequestedPartResponseDTO response = service.create(3, OrderRequestedPartCreateDTO.builder()
                .articleNumber("of123")
                .brand("BOSCH")
                .name("Oil Filter")
                .quantity(2)
                .build());

        assertThat(response.getStatus()).isEqualTo(OrderRequestedPartStatus.OUT_OF_STOCK);
        assertThat(response.getArticleNumber()).isEqualTo("OF123");
    }

    @Test
    void orderShouldFixPriceAndMoveStatus() {
        Order order = Order.builder().id(3).status(OrderStatus.NEW).build();
        OrderRequestedPart requested = OrderRequestedPart.builder()
                .id(11)
                .order(order)
                .articleNumber("OF123")
                .name("Oil Filter")
                .requestedQuantity(2)
                .status(OrderRequestedPartStatus.OUT_OF_STOCK)
                .build();
        when(orderRequestedPartRepository.findByIdAndOrderId(11, 3)).thenReturn(Optional.of(requested));
        when(orderRequestedPartRepository.save(any(OrderRequestedPart.class))).thenAnswer(inv -> inv.getArgument(0));

        OrderRequestedPartResponseDTO response = procurementService.order(3, 11, OrderRequestedPartOrderDTO.builder()
                .quote(CarretaQuoteOrderDTO.builder()
                        .positionSignature("sig-1")
                        .articleNumber("OF123")
                        .brand("BOSCH")
                        .name("Oil Filter")
                        .purchasePrice(BigDecimal.valueOf(100))
                        .deliveryDaysMin(1)
                        .deliveryDaysMax(2)
                        .minOrderQuantity(1)
                        .quantityRaw("10")
                        .build())
                .salePrice(BigDecimal.valueOf(150))
                .build());

        assertThat(response.getStatus()).isEqualTo(OrderRequestedPartStatus.ORDERED_IN_TRANSIT);
        assertThat(response.getSalePrice()).isEqualByComparingTo("150");
        verify(orderFinancialsService).recalculateAfterMutableTotalsChange(order);
    }

    @Test
    void receiveShouldReserveStockAndLinkLocalPart() {
        Order order = Order.builder().id(3).status(OrderStatus.NEW).build();
        OrderRequestedPart requested = OrderRequestedPart.builder()
                .id(11)
                .order(order)
                .articleNumber("OF123")
                .name("Oil Filter")
                .requestedQuantity(2)
                .salePrice(BigDecimal.valueOf(150))
                .status(OrderRequestedPartStatus.ORDERED_IN_TRANSIT)
                .build();
        Part local = Part.builder().id(7).articleNumber("OF123").name("Oil Filter").stockQuantity(5).reservedQuantity(1).cost(BigDecimal.TEN).build();
        when(orderRequestedPartRepository.findByIdAndOrderId(11, 3)).thenReturn(Optional.of(requested));
        when(partRepository.findById(7)).thenReturn(Optional.of(local));
        when(partRepository.save(any(Part.class))).thenAnswer(inv -> inv.getArgument(0));
        when(orderRequestedPartRepository.save(any(OrderRequestedPart.class))).thenAnswer(inv -> inv.getArgument(0));

        OrderRequestedPartResponseDTO response = receiptService.receive(3, 11, OrderRequestedPartReceiveDTO.builder()
                .targetPartId(7)
                .receivedQuantity(2)
                .build());

        assertThat(response.getStatus()).isEqualTo(OrderRequestedPartStatus.IN_STOCK_RESERVED);
        assertThat(local.getStockQuantity()).isEqualTo(7);
        assertThat(local.getReservedQuantity()).isEqualTo(3);
    }
}
