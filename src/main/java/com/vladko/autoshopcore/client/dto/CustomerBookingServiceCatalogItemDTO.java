package com.vladko.autoshopcore.client.dto;

import lombok.Builder;
import lombok.Value;

import java.math.BigDecimal;

@Value
@Builder
public class CustomerBookingServiceCatalogItemDTO {
    Integer id;
    String name;
    String description;
    BigDecimal basePrice;
    Integer categoryId;
    String categoryName;
    Integer defaultDurationMinutes;
}
