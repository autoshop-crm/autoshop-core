package com.vladko.autoshopcore.parts.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PartStockUpdateDTO {

    @NotNull
    @Min(0)
    private Integer stockQuantity;
}
