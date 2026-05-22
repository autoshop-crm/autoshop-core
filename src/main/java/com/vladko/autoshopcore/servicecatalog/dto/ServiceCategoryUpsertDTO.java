package com.vladko.autoshopcore.servicecatalog.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class ServiceCategoryUpsertDTO {
    @NotBlank
    @Size(max = 100)
    private String name;
    private Integer displayOrder;
    private Boolean active;
}
