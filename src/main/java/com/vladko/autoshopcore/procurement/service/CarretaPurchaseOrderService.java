package com.vladko.autoshopcore.procurement.service;

import com.vladko.autoshopcore.integration.carreta.client.CarretaClient;
import com.vladko.autoshopcore.integration.carreta.config.CarretaProperties;
import com.vladko.autoshopcore.integration.carreta.dto.CarretaOrderCreateRequest;
import com.vladko.autoshopcore.integration.carreta.dto.CarretaOrderResponse;
import com.vladko.autoshopcore.integration.carreta.support.CarretaQuantityParser;
import com.vladko.autoshopcore.procurement.dto.CarretaQuoteOrderDTO;
import com.vladko.autoshopcore.procurement.dto.PurchaseOrderCreateDTO;
import com.vladko.autoshopcore.procurement.dto.PurchaseOrderResponseDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

@Service
@RequiredArgsConstructor
public class CarretaPurchaseOrderService implements PurchaseOrderService {

    private final CarretaClient carretaClient;
    private final CarretaProperties properties;
    private final CarretaQuantityParser quantityParser;

    @Override
    public PurchaseOrderResponseDTO create(PurchaseOrderCreateDTO dto) {
        CarretaQuoteOrderDTO quote = dto.getQuote();
        quantityParser.validateOrderQuantity(quote.getQuantityRaw(), quote.getMinOrderQuantity(), dto.getQuantity());
        if (dto.getSalePrice().compareTo(quote.getPurchasePrice()) < 0) {
            throw new IllegalArgumentException("Sale price cannot be lower than purchase price");
        }

        boolean createExternalOrder = Boolean.TRUE.equals(dto.getCreateExternalOrder());
        CarretaOrderResponse externalOrder = null;
        if (createExternalOrder) {
            externalOrder = carretaClient.createOrder(toCarretaRequest(dto), properties.testOrdersEnabled());
        }

        BigDecimal purchaseTotal = quote.getPurchasePrice().multiply(BigDecimal.valueOf(dto.getQuantity()));
        BigDecimal saleTotal = dto.getSalePrice().multiply(BigDecimal.valueOf(dto.getQuantity()));

        return PurchaseOrderResponseDTO.builder()
                .provider("CARRETA")
                .externalOrderCreated(createExternalOrder)
                .testMode(createExternalOrder && properties.testOrdersEnabled())
                .externalOrderId(externalOrder == null ? null : externalOrder.getId())
                .externalOrderNumber(externalOrder == null ? null : externalOrder.getNumber())
                .externalStatus(externalOrder == null ? null : externalOrder.getStatus())
                .externalStatusDisplay(externalOrder == null ? null : externalOrder.getStatusDisplay())
                .articleNumber(quote.getArticleNumber())
                .brand(quote.getBrand())
                .name(quote.getName())
                .quantity(dto.getQuantity())
                .purchaseUnitPrice(quote.getPurchasePrice())
                .saleUnitPrice(dto.getSalePrice())
                .purchaseTotal(purchaseTotal)
                .saleTotal(saleTotal)
                .build();
    }

    private CarretaOrderCreateRequest toCarretaRequest(PurchaseOrderCreateDTO dto) {
        CarretaQuoteOrderDTO quote = dto.getQuote();
        return CarretaOrderCreateRequest.builder()
                .positionSignature(quote.getPositionSignature())
                .code(quote.getArticleNumber())
                .maker(quote.getBrand())
                .name(quote.getName())
                .price(quote.getPurchasePrice().toPlainString())
                .periodMin(quote.getDeliveryDaysMin())
                .periodMax(quote.getDeliveryDaysMax())
                .minQty(quote.getMinOrderQuantity())
                .qty(quote.getQuantityRaw())
                .orderQuantity(dto.getQuantity())
                .clientComment(dto.getClientComment())
                .build();
    }
}
