package com.vladko.autoshopcore.parts.dto.catalog;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CatalogArticleSearchResponseDTO {

    private String type;
    private Integer modificationId;
    @Builder.Default
    private List<Integer> productGroupIds = new ArrayList<>();
    private Integer supplierId;
    private int limit;
    private int offset;
    private boolean cached;
    private boolean fallback;
    private Instant cachedAt;
    private Instant cacheExpiresAt;
    @Builder.Default
    private List<CatalogArticleResponseDTO> items = new ArrayList<>();
}
