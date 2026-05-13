package com.vladko.autoshopcore.parts.dto.vehicle;

import com.vladko.autoshopcore.parts.dto.catalog.CatalogProductGroupResponseDTO;
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
public class VehicleScopedPartSearchResponseDTO {

    private Integer orderId;
    private Integer vehicleId;
    private String vehicleBrand;
    private String vehicleModel;
    private Integer modificationId;
    private String modificationName;
    private String query;
    private boolean catalogLinked;
    private boolean productGroupsCached;
    private boolean productGroupsFallback;
    private boolean articlesCached;
    private boolean articlesFallback;

    @Builder.Default
    private List<CatalogProductGroupResponseDTO> matchedProductGroups = new ArrayList<>();

    @Builder.Default
    private List<VehicleScopedPartSearchItemDTO> items = new ArrayList<>();
}
