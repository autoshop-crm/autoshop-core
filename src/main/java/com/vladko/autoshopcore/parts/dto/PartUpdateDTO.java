package com.vladko.autoshopcore.parts.dto;

import jakarta.validation.constraints.DecimalMin;
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
public class PartUpdateDTO {

    @Size(max = 20)
    private String brand;

    @Size(max = 50)
    private String name;

    @Size(max = 30)
    private String articleNumber;

    @DecimalMin("0.00")
    private BigDecimal cost;
}
