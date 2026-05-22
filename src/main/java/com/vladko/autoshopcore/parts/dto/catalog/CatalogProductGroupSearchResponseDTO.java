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
public class CatalogProductGroupSearchResponseDTO {

    private String type;
    private Integer modificationId;
    private String query;
    private boolean cached;
    private boolean fallback;
    private Instant cachedAt;
    private Instant cacheExpiresAt;
    @Builder.Default
    private List<CatalogProductGroupResponseDTO> items = new ArrayList<>();
}
