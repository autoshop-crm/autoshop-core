package com.vladko.autoshopcore.order.dto;

import com.vladko.autoshopcore.order.entity.OrderStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;

@Builder
@AllArgsConstructor
@NoArgsConstructor
@Data
public class OrderResponseDTO {
    private Integer id;
    private Integer customerId;
    private Integer vehicleId;
    private Integer employeeId;
    private String problem;
    private OrderStatus status;
    private BigDecimal laborTotal;
    private BigDecimal partsTotal;
    private BigDecimal costsTotal;
    private BigDecimal discountAmount;
    private BigDecimal finalAmount;
    private Instant createdAt;
    private Instant updatedAt;
    private Instant completedAt;
}
