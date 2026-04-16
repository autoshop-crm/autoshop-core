package com.vladko.autoshopcore.order.dto;

import com.vladko.autoshopcore.order.entity.OrderStatus;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Builder
@AllArgsConstructor
@NoArgsConstructor
@Data
public class OrderStatusUpdateDTO {
    @NotNull
    private OrderStatus status;
}
