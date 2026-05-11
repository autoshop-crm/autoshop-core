package com.vladko.autoshopcore.parts.dto;

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
public class OrderRequestedPartReceiveDTO {

    private Integer targetPartId;

    @Size(max = 20)
    private String brand;

    @Size(max = 100)
    private String name;

    @NotNull
    @Min(1)
    private Integer receivedQuantity;

    @DecimalMin("0.01")
    private BigDecimal salePrice;
}
