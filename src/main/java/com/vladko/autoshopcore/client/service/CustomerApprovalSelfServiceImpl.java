package com.vladko.autoshopcore.client.service;

import com.vladko.autoshopcore.order.approval.dto.OrderApprovalDecisionCreateDTO;
import com.vladko.autoshopcore.order.approval.dto.OrderApprovalRequestResponseDTO;
import com.vladko.autoshopcore.order.approval.service.OrderApprovalService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CustomerApprovalSelfServiceImpl implements CustomerApprovalSelfService {

    private final OrderApprovalService orderApprovalService;

    @Override
    @Transactional(readOnly = true)
    public List<OrderApprovalRequestResponseDTO> getCurrentCustomerOrderApprovals(Integer orderId) {
        return orderApprovalService.getByOrderId(orderId);
    }

    @Override
    @Transactional
    public OrderApprovalRequestResponseDTO approve(Integer orderId, Integer requestId, OrderApprovalDecisionCreateDTO dto) {
        return orderApprovalService.approve(orderId, requestId, dto);
    }

    @Override
    @Transactional
    public OrderApprovalRequestResponseDTO reject(Integer orderId, Integer requestId, OrderApprovalDecisionCreateDTO dto) {
        return orderApprovalService.reject(orderId, requestId, dto);
    }
}
