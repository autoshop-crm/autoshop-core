package com.vladko.autoshopcore.servicecatalog.dto;

import lombok.Builder;
import lombok.Value;

import java.math.BigDecimal;
import java.util.List;

@Value
@Builder
public class ServiceCatalogItemResponseDTO {
    Integer id;
    String name;
    String description;
    BigDecimal basePrice;
    Boolean active;
    Integer categoryId;
    String categoryName;
    Integer defaultDurationMinutes;
    List<String> inspectionItems;
}
