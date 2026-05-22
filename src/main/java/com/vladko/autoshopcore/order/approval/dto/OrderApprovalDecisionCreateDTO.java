package com.vladko.autoshopcore.order.approval.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class OrderApprovalDecisionCreateDTO {
    @NotBlank
    private String decisionToken;
    @Size(max = 1000)
    private String comment;
}
