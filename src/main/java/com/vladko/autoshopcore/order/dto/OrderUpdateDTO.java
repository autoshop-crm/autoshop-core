package com.vladko.autoshopcore.order.dto;

import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Builder
@AllArgsConstructor
@NoArgsConstructor
@Data
public class OrderUpdateDTO {
    @Pattern(regexp = "^(?!\\s*$).{1,1000}$")
    private String problem;

    private Integer employeeId;
}
