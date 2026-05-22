package com.vladko.autoshopcore.integration.umapi.mapper;

import com.vladko.autoshopcore.integration.umapi.dto.UmapiArticleItem;
import com.vladko.autoshopcore.parts.dto.catalog.CatalogArticleResponseDTO;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.List;

@Component
public class UmapiCatalogArticleMapper {

    public CatalogArticleResponseDTO map(UmapiArticleItem item) {
        String articleNumber = firstText(item.getArticleNumber(), item.getLinkedDisplayNumber());
        return CatalogArticleResponseDTO.builder()
                .source("UMAPI_AUTOCATALOG")
                .umapiArticleId(item.getArticleId())
                .articleNumber(articleNumber)
                .brandId(item.getSupplierId())
                .brand(firstText(item.getBrand(), item.getLinkedBrand()))
                .name(item.getCompleteDescription())
                .shortDescription(item.getDescription())
                .oeCodes(nullSafe(item.getOeCodes()))
                .eanCodes(nullSafe(item.getEanCodes()))
                .mediaFile(item.getMediaFile())
                .supplierQuoteSearchUrl(StringUtils.hasText(articleNumber)
                        ? "/api/procurement/supplier-quotes/search?query=%s".formatted(articleNumber)
                        : null)
                .build();
    }

    private List<String> nullSafe(List<String> values) {
        return values == null ? List.of() : values;
    }

    private String firstText(String primary, String fallback) {
        return StringUtils.hasText(primary) ? primary : fallback;
    }
}
