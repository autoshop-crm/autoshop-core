package com.vladko.autoshopcore.event.notification;

import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

class OrderNumberFormatterTest {

    private final OrderNumberFormatter formatter = new OrderNumberFormatter();

    @Test
    void formatShouldUseYearAndPaddedOrderId() {
        assertThat(formatter.format(42, Instant.parse("2026-04-22T10:15:30Z")))
                .isEqualTo("AS-2026-00042");
    }

    @Test
    void formatShouldNotTrimLargeOrderIds() {
        assertThat(formatter.format(100001, Instant.parse("2026-04-22T10:15:30Z")))
                .isEqualTo("AS-2026-100001");
    }
}
