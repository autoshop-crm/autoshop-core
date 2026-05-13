package com.vladko.autoshopcore.parts.dto.vehicle;

import com.vladko.autoshopcore.parts.dto.PartResponseDTO;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VehicleScopedPartSearchItemDTO {

    private Integer productGroupId;
    private String productGroupName;
    private Integer umapiArticleId;
    private String articleNumber;
    private String brand;
    private String name;
    private String shortDescription;
    private String source;
    private String mediaFile;
    private String supplierQuoteSearchUrl;
    private PartResponseDTO matchedLocalPart;
    private boolean exactLocalMatch;
    private boolean availableLocally;
    private boolean canAddAsLocal;
    private boolean canAddAsRequested;
}
