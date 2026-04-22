package com.vladko.autoshopcore.integration.umapi.mapper;

import com.vladko.autoshopcore.integration.umapi.dto.UmapiArticleItem;
import com.vladko.autoshopcore.parts.dto.ExternalPartCatalogItemResponseDTO;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
public class UmapiCatalogItemMapper {

    public ExternalPartCatalogItemResponseDTO map(UmapiArticleItem item) {
        return ExternalPartCatalogItemResponseDTO.builder()
                .source("UMAPI_AUTOCATALOG")
                .umapiArticleId(item.getArticleId())
                .articleNumber(firstText(item.getArticleNumber(), item.getLinkedDisplayNumber()))
                .brandId(item.getSupplierId())
                .brand(firstText(item.getBrand(), item.getLinkedBrand()))
                .name(item.getCompleteDescription())
                .shortDescription(item.getDescription())
                .status(item.getStatusDescription())
                .mediaFile(item.getMediaFile())
                .build();
    }

    private String firstText(String primary, String fallback) {
        return StringUtils.hasText(primary) ? primary : fallback;
    }
}
