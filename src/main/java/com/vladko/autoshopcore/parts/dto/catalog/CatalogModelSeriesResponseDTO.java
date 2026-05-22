package com.vladko.autoshopcore.parts.dto.catalog;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CatalogModelSeriesResponseDTO {

    private String type;
    private Integer manufacturerId;
    private Integer modelSeriesId;
    private String name;
    private String productionFrom;
    private String productionTo;
}
