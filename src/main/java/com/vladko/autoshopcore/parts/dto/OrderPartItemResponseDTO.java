package com.vladko.autoshopcore.parts.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderPartItemResponseDTO {

    private Integer id;
    private Integer orderId;
    private Integer partId;
    private String articleNumber;
    private String brand;
    private String name;
    private Integer quantity;
    private BigDecimal unitPrice;
    private BigDecimal lineTotal;
}
