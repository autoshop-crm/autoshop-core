package com.vladko.autoshopcore.procurement.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CarretaQuoteOrderDTO {

    @NotBlank
    private String positionSignature;

    @NotBlank
    private String articleNumber;

    @NotBlank
    private String brand;

    @NotBlank
    private String name;

    @NotNull
    @DecimalMin("0.01")
    private BigDecimal purchasePrice;

    @NotNull
    private Integer deliveryDaysMin;

    @NotNull
    private Integer deliveryDaysMax;

    @NotNull
    @Min(1)
    private Integer minOrderQuantity;

    @NotBlank
    private String quantityRaw;
}
