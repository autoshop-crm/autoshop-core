package com.vladko.autoshopcore.integration.carreta.support;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CarretaQuantityParserTest {

    private final CarretaQuantityParser parser = new CarretaQuantityParser();

    @Test
    void parseShouldHandleExactQuantity() {
        CarretaQuantityParser.ParsedQuantity parsedQuantity = parser.parse("500");

        assertThat(parsedQuantity.mode()).isEqualTo(CarretaQuantityParser.QuantityMode.EXACT);
        assertThat(parsedQuantity.value()).isEqualTo(500);
        assertThat(parser.parseExactAvailableQuantity("500")).isEqualTo(500);
    }

    @Test
    void parseShouldHandleLessThanQuantity() {
        CarretaQuantityParser.ParsedQuantity parsedQuantity = parser.parse("<10");

        assertThat(parsedQuantity.mode()).isEqualTo(CarretaQuantityParser.QuantityMode.LESS_THAN);
        assertThat(parsedQuantity.value()).isEqualTo(10);
        assertThat(parser.parseExactAvailableQuantity("<10")).isNull();
    }

    @Test
    void validateOrderQuantityShouldRejectValuesBreakingCarretaRules() {
        assertThatThrownBy(() -> parser.validateOrderQuantity("10", 1, 11))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Order quantity cannot exceed available quantity");

        assertThatThrownBy(() -> parser.validateOrderQuantity("<10", 1, 10))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Order quantity must be lower than available quantity marker");

        assertThatThrownBy(() -> parser.validateOrderQuantity("500", 2, 3))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Order quantity must be a multiple of minimum quantity");
    }
}
