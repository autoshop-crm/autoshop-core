package com.vladko.autoshopcore.parts.dto;

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
public class ExternalPartSearchResponseDTO {

    private String articleNumber;
    private String brand;
    private boolean cached;
    private boolean fallback;
    private Instant cachedAt;
    private Instant cacheExpiresAt;

    @Builder.Default
    private List<ExternalPartCatalogItemResponseDTO> items = new ArrayList<>();
}
