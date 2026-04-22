package com.vladko.autoshopcore.procurement.service;

import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;

@Service
public class MarkupPricingService {

    public BigDecimal calculateSalePrice(BigDecimal purchasePrice) {
        if (purchasePrice == null || purchasePrice.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Purchase price must be positive");
        }

        BigDecimal multiplier;
        if (purchasePrice.compareTo(new BigDecimal("1000.00")) < 0) {
            multiplier = new BigDecimal("1.35");
        } else if (purchasePrice.compareTo(new BigDecimal("5000.00")) <= 0) {
            multiplier = new BigDecimal("1.25");
        } else {
            multiplier = new BigDecimal("1.15");
        }

        return purchasePrice.multiply(multiplier).setScale(2, RoundingMode.HALF_UP);
    }
}
