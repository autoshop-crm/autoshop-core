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
public class OrderPartItemCreateDTO {

    @NotNull
    private Integer partId;

    @NotNull
    @Min(1)
    private Integer quantity;
}
