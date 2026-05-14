package com.vladko.autoshopcore.order.approval.controller;

import com.vladko.autoshopcore.order.approval.dto.OrderApprovalDecisionCreateDTO;
import com.vladko.autoshopcore.order.approval.dto.OrderApprovalRequestCreateDTO;
import com.vladko.autoshopcore.order.approval.dto.OrderApprovalRequestResponseDTO;
import com.vladko.autoshopcore.order.approval.service.OrderApprovalService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/orders/{orderId}/approvals")
@RequiredArgsConstructor
public class OrderApprovalController {

    private final OrderApprovalService orderApprovalService;

    @PostMapping
    public ResponseEntity<OrderApprovalRequestResponseDTO> requestApproval(@PathVariable Integer orderId,
                                                                           @Valid @RequestBody OrderApprovalRequestCreateDTO dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(orderApprovalService.requestApproval(orderId, dto));
    }

    @PostMapping("/{requestId}/approve")
    public ResponseEntity<OrderApprovalRequestResponseDTO> approve(@PathVariable Integer orderId,
                                                                   @PathVariable Integer requestId,
                                                                   @Valid @RequestBody OrderApprovalDecisionCreateDTO dto) {
        return ResponseEntity.ok(orderApprovalService.approve(orderId, requestId, dto));
    }

    @PostMapping("/{requestId}/reject")
    public ResponseEntity<OrderApprovalRequestResponseDTO> reject(@PathVariable Integer orderId,
                                                                  @PathVariable Integer requestId,
                                                                  @Valid @RequestBody OrderApprovalDecisionCreateDTO dto) {
        return ResponseEntity.ok(orderApprovalService.reject(orderId, requestId, dto));
    }

    @GetMapping
    public ResponseEntity<List<OrderApprovalRequestResponseDTO>> getAll(@PathVariable Integer orderId) {
        return ResponseEntity.ok(orderApprovalService.getByOrderId(orderId));
    }
}
