package com.vladko.autoshopcore.integration.carreta.mapper;

import com.vladko.autoshopcore.integration.carreta.config.CarretaProperties;
import com.vladko.autoshopcore.integration.carreta.dto.CarretaSearchItemResponse;
import com.vladko.autoshopcore.integration.carreta.support.CarretaQuantityParser;
import com.vladko.autoshopcore.procurement.dto.SupplierQuoteResponseDTO;
import com.vladko.autoshopcore.procurement.service.MarkupPricingService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.Instant;

@Component
@RequiredArgsConstructor
public class CarretaQuoteMapper {

    private final CarretaProperties properties;
    private final CarretaQuantityParser quantityParser;
    private final MarkupPricingService markupPricingService;

    public SupplierQuoteResponseDTO map(String requestedCode, CarretaSearchItemResponse item, Instant fetchedAt) {
        BigDecimal purchasePrice = parseMoney(item.getPrice());
        BigDecimal salePrice = markupPricingService.calculateSalePrice(purchasePrice);

        return SupplierQuoteResponseDTO.builder()
                .provider("CARRETA")
                .sourceCode(item.getSource())
                .requestedCode(requestedCode)
                .articleNumber(normalizeText(item.getCode()))
                .brand(normalizeText(item.getMaker()))
                .name(normalizeText(item.getName()))
                .description(normalizeText(item.getDesc()))
                .cross(Boolean.TRUE.equals(item.getIsCross()))
                .purchasePrice(purchasePrice)
                .currency("RUB")
                .quantityRaw(item.getQty())
                .availableQuantityParsed(quantityParser.parseExactAvailableQuantity(item.getQty()))
                .minOrderQuantity(item.getMinQty())
                .deliveryDaysMin(item.getPeriodMin())
                .deliveryDaysMax(item.getPeriodMax())
                .supplyProbabilityPercent(item.getStat())
                .recommendedSalePrice(salePrice)
                .marginAmount(salePrice.subtract(purchasePrice))
                .fetchedAt(fetchedAt)
                .expiresAt(fetchedAt.plus(properties.cache().searchTtl()))
                .build();
    }

    private BigDecimal parseMoney(String value) {
        if (value == null) {
            throw new IllegalArgumentException("Supplier quote price must not be null");
        }
        return new BigDecimal(value.trim());
    }

    private String normalizeText(String value) {
        if (value == null) {
            return null;
        }
        String normalizedValue = value.trim();
        return normalizedValue.isEmpty() ? null : normalizedValue;
    }
}
