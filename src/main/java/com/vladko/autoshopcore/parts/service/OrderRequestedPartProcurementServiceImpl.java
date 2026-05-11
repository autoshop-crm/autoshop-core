package com.vladko.autoshopcore.parts.service;

import com.vladko.autoshopcore.order.entity.Order;
import com.vladko.autoshopcore.order.exception.InvalidOrderStateException;
import com.vladko.autoshopcore.order.repository.OrderRepository;
import com.vladko.autoshopcore.order.service.OrderFinancialsService;
import com.vladko.autoshopcore.parts.dto.OrderRequestedPartOrderDTO;
import com.vladko.autoshopcore.parts.dto.OrderRequestedPartResponseDTO;
import com.vladko.autoshopcore.parts.entity.OrderRequestedPart;
import com.vladko.autoshopcore.parts.entity.OrderRequestedPartStatus;
import com.vladko.autoshopcore.parts.exception.OrderRequestedPartNotFoundException;
import com.vladko.autoshopcore.parts.repository.OrderRequestedPartRepository;
import com.vladko.autoshopcore.procurement.dto.PurchaseOrderCreateDTO;
import com.vladko.autoshopcore.procurement.service.PurchaseOrderService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Service
@RequiredArgsConstructor
public class OrderRequestedPartProcurementServiceImpl implements OrderRequestedPartProcurementService {

    private final OrderRequestedPartRepository orderRequestedPartRepository;
    private final PurchaseOrderService purchaseOrderService;
    private final OrderRepository orderRepository;
    private final OrderFinancialsService orderFinancialsService;
    private final OrderRequestedPartMapper mapper;

    @Override
    @Transactional
    public OrderRequestedPartResponseDTO order(Integer orderId, Integer requestedPartId, OrderRequestedPartOrderDTO dto) {
        OrderRequestedPart requestedPart = orderRequestedPartRepository.findByIdAndOrderId(requestedPartId, orderId)
                .orElseThrow(() -> new OrderRequestedPartNotFoundException(orderId, requestedPartId));
        Order order = requestedPart.getOrder();
        ensureEditable(order);
        if (requestedPart.getStatus() != OrderRequestedPartStatus.OUT_OF_STOCK) {
            throw new InvalidOrderStateException("Requested part must be OUT_OF_STOCK before ordering");
        }

        purchaseOrderService.create(PurchaseOrderCreateDTO.builder()
                .quote(dto.getQuote())
                .quantity(requestedPart.getRequestedQuantity())
                .salePrice(dto.getSalePrice())
                .clientComment(dto.getClientComment())
                .createExternalOrder(dto.getCreateExternalOrder())
                .build());

        requestedPart.setStatus(OrderRequestedPartStatus.ORDERED_IN_TRANSIT);
        requestedPart.setSelectedSupplier(dto.getQuote().getPositionSignature() == null ? "CARRETA" : "CARRETA");
        requestedPart.setSelectedQuoteSignature(dto.getQuote().getPositionSignature());
        requestedPart.setPurchasePrice(dto.getQuote().getPurchasePrice());
        requestedPart.setSalePrice(dto.getSalePrice());
        requestedPart.setDeliveryDaysMin(dto.getQuote().getDeliveryDaysMin());
        requestedPart.setDeliveryDaysMax(dto.getQuote().getDeliveryDaysMax());
        requestedPart.setCurrency("RUB");
        requestedPart.setQuoteFetchedAt(Instant.now());
        requestedPart.setOrderedAt(Instant.now());

        orderFinancialsService.recalculateAfterMutableTotalsChange(order);
        orderRepository.save(order);

        return mapper.map(orderRequestedPartRepository.save(requestedPart));
    }

    private void ensureEditable(Order order) {
        switch (order.getStatus()) {
            case NEW, IN_PROGRESS -> { }
            default -> throw new InvalidOrderStateException("Order in status '%s' can no longer be updated".formatted(order.getStatus()));
        }
    }
}
