package com.vladko.autoshopcore.parts.dto;

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
public class UnifiedPartSearchResponseDTO {

    private String articleNumber;
    private String brand;
    private boolean externalCached;
    private boolean externalFallback;

    @Builder.Default
    private List<UnifiedPartSearchItemResponseDTO> items = new ArrayList<>();
}
