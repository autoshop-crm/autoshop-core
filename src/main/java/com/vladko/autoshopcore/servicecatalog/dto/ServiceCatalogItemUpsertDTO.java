package com.vladko.autoshopcore.servicecatalog.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
public class ServiceCatalogItemUpsertDTO {
    @NotBlank
    @Size(max = 100)
    private String name;

    @Size(max = 255)
    private String description;

    @NotNull
    @DecimalMin("0.00")
    private BigDecimal basePrice;

    private Integer categoryId;

    private Boolean active;

    private Integer defaultDurationMinutes;

    private List<@NotBlank @Size(max = 255) String> inspectionItems;
}
