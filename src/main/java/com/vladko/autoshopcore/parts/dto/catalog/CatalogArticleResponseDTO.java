package com.vladko.autoshopcore.parts.dto.catalog;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CatalogArticleResponseDTO {

    private String source;
    private Integer umapiArticleId;
    private String articleNumber;
    private Integer brandId;
    private String brand;
    private String name;
    private String shortDescription;
    @Builder.Default
    private List<String> oeCodes = new ArrayList<>();
    @Builder.Default
    private List<String> eanCodes = new ArrayList<>();
    private String mediaFile;
    private Integer localPartId;
    private Integer localStockQuantity;
    private Integer localAvailableQuantity;
    private String supplierQuoteSearchUrl;
}
