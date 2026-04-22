package com.vladko.autoshopcore.vehicle.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VehicleCatalogLinkDTO {

    @NotBlank
    private String type;

    @NotNull
    private Integer manufacturerId;

    @NotBlank
    private String manufacturerName;

    @NotNull
    private Integer modelSeriesId;

    @NotBlank
    private String modelSeriesName;

    @NotNull
    private Integer modificationId;

    @NotBlank
    private String modificationName;

    private String engineDescription;
}
