package com.vladko.autoshopcore.parts.controller;

import com.vladko.autoshopcore.parts.dto.OrderRequestedPartOrderDTO;
import com.vladko.autoshopcore.parts.dto.OrderRequestedPartReceiveDTO;
import com.vladko.autoshopcore.parts.dto.OrderRequestedPartResponseDTO;
import com.vladko.autoshopcore.parts.service.OrderRequestedPartProcurementService;
import com.vladko.autoshopcore.parts.service.OrderRequestedPartReceiptService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/orders/{orderId}/requested-parts/{requestedPartId}")
@RequiredArgsConstructor
public class OrderRequestedPartProcurementController {

    private final OrderRequestedPartProcurementService procurementService;
    private final OrderRequestedPartReceiptService receiptService;

    @PostMapping("/order")
    public ResponseEntity<OrderRequestedPartResponseDTO> order(@PathVariable Integer orderId,
                                                               @PathVariable Integer requestedPartId,
                                                               @Valid @RequestBody OrderRequestedPartOrderDTO dto) {
        return ResponseEntity.ok(procurementService.order(orderId, requestedPartId, dto));
    }

    @PostMapping("/receive")
    public ResponseEntity<OrderRequestedPartResponseDTO> receive(@PathVariable Integer orderId,
                                                                 @PathVariable Integer requestedPartId,
                                                                 @Valid @RequestBody OrderRequestedPartReceiveDTO dto) {
        return ResponseEntity.ok(receiptService.receive(orderId, requestedPartId, dto));
    }
}
