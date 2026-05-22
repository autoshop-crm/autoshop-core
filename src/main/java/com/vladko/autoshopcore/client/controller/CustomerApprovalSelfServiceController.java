package com.vladko.autoshopcore.client.controller;

import com.vladko.autoshopcore.client.service.CustomerApprovalSelfService;
import com.vladko.autoshopcore.order.approval.dto.OrderApprovalDecisionCreateDTO;
import com.vladko.autoshopcore.order.approval.dto.OrderApprovalRequestResponseDTO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/customers/me/orders/{orderId}/approvals")
@RequiredArgsConstructor
public class CustomerApprovalSelfServiceController {

    private final CustomerApprovalSelfService customerApprovalSelfService;

    @GetMapping
    public ResponseEntity<List<OrderApprovalRequestResponseDTO>> getCurrentCustomerOrderApprovals(@PathVariable Integer orderId) {
        return ResponseEntity.ok(customerApprovalSelfService.getCurrentCustomerOrderApprovals(orderId));
    }

    @PostMapping("/{requestId}/approve")
    public ResponseEntity<OrderApprovalRequestResponseDTO> approve(@PathVariable Integer orderId,
                                                                   @PathVariable Integer requestId,
                                                                   @Valid @RequestBody OrderApprovalDecisionCreateDTO dto) {
        return ResponseEntity.ok(customerApprovalSelfService.approve(orderId, requestId, dto));
    }

    @PostMapping("/{requestId}/reject")
    public ResponseEntity<OrderApprovalRequestResponseDTO> reject(@PathVariable Integer orderId,
                                                                  @PathVariable Integer requestId,
                                                                  @Valid @RequestBody OrderApprovalDecisionCreateDTO dto) {
        return ResponseEntity.ok(customerApprovalSelfService.reject(orderId, requestId, dto));
    }
}
