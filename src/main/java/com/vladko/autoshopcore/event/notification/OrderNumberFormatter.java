package com.vladko.autoshopcore.event.notification;

import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.ZoneOffset;

@Component
public class OrderNumberFormatter {

    public String format(Integer orderId, Instant createdAt) {
        if (orderId == null) {
            throw new IllegalArgumentException("orderId must not be null");
        }

        int year = (createdAt == null ? Instant.now() : createdAt)
                .atZone(ZoneOffset.UTC)
                .getYear();
        String paddedId = orderId < 100000 ? "%05d".formatted(orderId) : orderId.toString();
        return "AS-%d-%s".formatted(year, paddedId);
    }
}
