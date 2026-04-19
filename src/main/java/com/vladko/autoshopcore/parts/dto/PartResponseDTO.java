package com.vladko.autoshopcore.parts.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PartResponseDTO {

    private Integer id;
    private String brand;
    private String name;
    private String articleNumber;
    private BigDecimal cost;
    private Integer stockQuantity;
    private Integer reservedQuantity;
    private Integer availableQuantity;
    private Instant createdAt;
    private Instant updatedAt;
}
