package com.vladko.autoshopcore.parts.dto.catalog;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CatalogModificationResponseDTO {

    private String type;
    private Integer modelSeriesId;
    private Integer modificationId;
    private String name;
    private Integer powerPs;
    private BigDecimal capacityLiters;
    private String engineType;
    private String bodyType;
    private String fuelType;
    private String displayName;
}
