package com.vladko.autoshopcore.parts.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ExternalPartCatalogItemResponseDTO {

    private String source;
    private Integer umapiArticleId;
    private String articleNumber;
    private Integer brandId;
    private String brand;
    private String name;
    private String shortDescription;
    private String status;
    private String mediaFile;
}
