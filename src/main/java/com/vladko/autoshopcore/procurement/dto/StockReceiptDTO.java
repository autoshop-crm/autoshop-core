package com.vladko.autoshopcore.procurement.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
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
public class StockReceiptDTO {

    @NotNull
    private Integer targetPartId;

    @NotNull
    @Min(1)
    private Integer receivedQuantity;

    @DecimalMin("0.01")
    private BigDecimal salePrice;
}
