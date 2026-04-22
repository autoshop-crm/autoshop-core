package com.vladko.autoshopcore.integration.umapi.mapper;

import com.vladko.autoshopcore.integration.umapi.dto.catalog.UmapiFuseProductGroupResponse;
import com.vladko.autoshopcore.parts.dto.catalog.CatalogProductGroupResponseDTO;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;

@Component
public class UmapiProductGroupMapper {

    public CatalogProductGroupResponseDTO map(UmapiFuseProductGroupResponse item, BigDecimal score) {
        return CatalogProductGroupResponseDTO.builder()
                .productGroupId(item.getProductGroupId())
                .name(item.getDescription())
                .normalizedName(firstText(item.getNormalizedDescription(), item.getDescription()))
                .score(score)
                .build();
    }

    private String firstText(String primary, String fallback) {
        return StringUtils.hasText(primary) ? primary : fallback;
    }
}
