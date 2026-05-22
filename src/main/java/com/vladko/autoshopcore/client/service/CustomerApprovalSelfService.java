package com.vladko.autoshopcore.client.service;

import com.vladko.autoshopcore.order.approval.dto.OrderApprovalDecisionCreateDTO;
import com.vladko.autoshopcore.order.approval.dto.OrderApprovalRequestResponseDTO;

import java.util.List;

public interface CustomerApprovalSelfService {
    List<OrderApprovalRequestResponseDTO> getCurrentCustomerOrderApprovals(Integer orderId);
    OrderApprovalRequestResponseDTO approve(Integer orderId, Integer requestId, OrderApprovalDecisionCreateDTO dto);
    OrderApprovalRequestResponseDTO reject(Integer orderId, Integer requestId, OrderApprovalDecisionCreateDTO dto);
}
