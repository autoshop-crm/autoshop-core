package com.vladko.autoshopcore.order.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Builder
@AllArgsConstructor
@NoArgsConstructor
@Data
public class OrderCreateDTO {
    @NotNull
    private Integer customerId;

    @NotNull
    private Integer vehicleId;

    private Integer employeeId;

    @NotBlank
    @Size(max = 1000)
    private String problem;
}
