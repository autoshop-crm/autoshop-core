package com.vladko.autoshopcore.procurement.dto;

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
public class SupplierQuoteSearchResponseDTO {

    private String query;
    private String provider;
    private boolean cached;
    private boolean fallback;
    private Instant cachedAt;
    private Instant cacheExpiresAt;

    @Builder.Default
    private List<SupplierQuoteResponseDTO> quotes = new ArrayList<>();
}
