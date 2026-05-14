package com.vladko.autoshopcore.order.approval.service;

import com.vladko.autoshopcore.order.approval.dto.OrderApprovalDecisionCreateDTO;
import com.vladko.autoshopcore.order.approval.dto.OrderApprovalRequestCreateDTO;
import com.vladko.autoshopcore.order.approval.dto.OrderApprovalRequestResponseDTO;

import java.util.List;

public interface OrderApprovalService {
    OrderApprovalRequestResponseDTO requestApproval(Integer orderId, OrderApprovalRequestCreateDTO dto);
    OrderApprovalRequestResponseDTO approve(Integer orderId, Integer requestId, OrderApprovalDecisionCreateDTO dto);
    OrderApprovalRequestResponseDTO reject(Integer orderId, Integer requestId, OrderApprovalDecisionCreateDTO dto);
    List<OrderApprovalRequestResponseDTO> getByOrderId(Integer orderId);
}
