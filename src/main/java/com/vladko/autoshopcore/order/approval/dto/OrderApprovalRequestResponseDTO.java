package com.vladko.autoshopcore.order.approval.dto;

import com.vladko.autoshopcore.order.approval.entity.OrderApprovalRequestStatus;
import com.vladko.autoshopcore.order.approval.entity.OrderApprovalType;
import com.vladko.autoshopcore.order.approval.entity.OrderWorkProposalStatus;
import com.vladko.autoshopcore.parts.dto.OrderRequestedPartResponseDTO;
import lombok.Builder;
import lombok.Value;

import java.math.BigDecimal;
import java.time.Instant;

@Value
@Builder
public class OrderApprovalRequestResponseDTO {
    Integer requestId;
    Integer orderId;
    Integer proposalId;
    OrderApprovalType approvalType;
    OrderApprovalRequestStatus requestStatus;
    OrderWorkProposalStatus proposalStatus;
    String requestToken;
    String title;
    String description;
    BigDecimal laborAmount;
    BigDecimal partsAmount;
    BigDecimal totalAmount;
    Instant requestedAt;
    Instant expiresAt;
    String customerContactChannel;
    OrderRequestedPartResponseDTO requestedPart;
}
