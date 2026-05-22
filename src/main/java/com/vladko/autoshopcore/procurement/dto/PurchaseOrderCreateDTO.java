package com.vladko.autoshopcore.procurement.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PurchaseOrderCreateDTO {

    @NotNull
    @Valid
    private CarretaQuoteOrderDTO quote;

    @NotNull
    @Min(1)
    private Integer quantity;

    @NotNull
    @DecimalMin("0.01")
    private BigDecimal salePrice;

    @Size(max = 50)
    private String clientComment;

    @Builder.Default
    private Boolean createExternalOrder = Boolean.TRUE;
}
