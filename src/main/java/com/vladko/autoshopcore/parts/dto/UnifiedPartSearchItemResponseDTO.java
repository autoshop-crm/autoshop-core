package com.vladko.autoshopcore.parts.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UnifiedPartSearchItemResponseDTO {

    private String sourceType;
    private String articleNumber;
    private String brand;
    private String name;
    private PartResponseDTO localPart;
    private ExternalPartCatalogItemResponseDTO externalPart;
    private PartResponseDTO matchedLocalPart;
    private boolean exactLocalMatch;
    private boolean availableLocally;
}
