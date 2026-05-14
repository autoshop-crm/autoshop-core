package com.vladko.autoshopcore.order.approval.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class OrderApprovalRequestCreateDTO {
    @NotBlank
    @Size(max = 255)
    private String title;
    @Size(max = 1000)
    private String description;
    @NotNull
    @DecimalMin("0.00")
    private BigDecimal laborAmount;
    @NotNull
    @DecimalMin("0.00")
    private BigDecimal partsAmount;
    private Boolean requiresApproval;
    @Valid
    private ApprovalRequestedPartDTO requestedPart;
    private String customerContactChannel;
}
