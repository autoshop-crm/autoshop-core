package com.vladko.autoshopcore.integration.carreta.support;

import lombok.Getter;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
public class CarretaQuantityParser {

    private static final int UNKNOWN_QUANTITY_LIMIT = 10_000;

    public ParsedQuantity parse(String rawValue) {
        if (!StringUtils.hasText(rawValue)) {
            return new ParsedQuantity(QuantityMode.UNKNOWN, null);
        }

        String value = rawValue.trim();
        if (value.startsWith("<")) {
            return new ParsedQuantity(QuantityMode.LESS_THAN, parseInteger(value.substring(1)));
        }
        if (value.startsWith(">")) {
            return new ParsedQuantity(QuantityMode.GREATER_THAN, parseInteger(value.substring(1)));
        }
        if (value.toLowerCase().contains("есть")) {
            return new ParsedQuantity(QuantityMode.IN_STOCK_UNKNOWN, null);
        }
        return new ParsedQuantity(QuantityMode.EXACT, parseInteger(value));
    }

    public Integer parseExactAvailableQuantity(String rawValue) {
        ParsedQuantity parsedQuantity = parse(rawValue);
        return parsedQuantity.mode == QuantityMode.EXACT ? parsedQuantity.value : null;
    }

    public void validateOrderQuantity(String rawValue, Integer minQuantity, Integer orderQuantity) {
        if (orderQuantity == null || orderQuantity <= 0) {
            throw new IllegalArgumentException("Order quantity must be greater than zero");
        }

        int normalizedMinQuantity = minQuantity == null || minQuantity <= 0 ? 1 : minQuantity;
        if (orderQuantity % normalizedMinQuantity != 0) {
            throw new IllegalArgumentException("Order quantity must be a multiple of minimum quantity");
        }

        ParsedQuantity parsedQuantity = parse(rawValue);
        switch (parsedQuantity.mode) {
            case EXACT -> {
                if (parsedQuantity.value != null && orderQuantity > parsedQuantity.value) {
                    throw new IllegalArgumentException("Order quantity cannot exceed available quantity");
                }
            }
            case LESS_THAN -> {
                if (parsedQuantity.value != null && orderQuantity >= parsedQuantity.value) {
                    throw new IllegalArgumentException("Order quantity must be lower than available quantity marker");
                }
            }
            case GREATER_THAN, IN_STOCK_UNKNOWN, UNKNOWN -> {
                if (orderQuantity > UNKNOWN_QUANTITY_LIMIT) {
                    throw new IllegalArgumentException("Order quantity is too large for unknown stock");
                }
            }
        }
    }

    private Integer parseInteger(String value) {
        try {
            return Integer.parseInt(value.trim());
        } catch (NumberFormatException exception) {
            return null;
        }
    }

    public record ParsedQuantity(QuantityMode mode, Integer value) {
    }

    @Getter
    public enum QuantityMode {
        EXACT,
        LESS_THAN,
        GREATER_THAN,
        IN_STOCK_UNKNOWN,
        UNKNOWN
    }
}
