package com.vladko.autoshopcore.order.dto;

import lombok.Builder;
import lombok.Value;

import java.math.BigDecimal;

@Value
@Builder
public class OrderServiceLineDTO {
    Integer serviceId;
    String serviceName;
    BigDecimal price;
}
