package com.vladko.autoshopcore.order.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Builder
@AllArgsConstructor
@NoArgsConstructor
@Data
public class OrderEstimateUpdateDTO {
    @NotNull
    @DecimalMin("0.00")
    private BigDecimal costsTotal;

    @NotNull
    @DecimalMin("0.00")
    private BigDecimal discountAmount;
}
