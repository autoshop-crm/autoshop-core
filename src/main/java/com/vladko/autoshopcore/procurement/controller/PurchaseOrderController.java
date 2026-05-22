package com.vladko.autoshopcore.procurement.controller;

import com.vladko.autoshopcore.procurement.dto.PurchaseOrderCreateDTO;
import com.vladko.autoshopcore.procurement.dto.PurchaseOrderResponseDTO;
import com.vladko.autoshopcore.procurement.service.PurchaseOrderService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/procurement/purchase-orders")
@RequiredArgsConstructor
public class PurchaseOrderController {

    private final PurchaseOrderService purchaseOrderService;

    @PostMapping
    public ResponseEntity<PurchaseOrderResponseDTO> create(@Valid @RequestBody PurchaseOrderCreateDTO dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(purchaseOrderService.create(dto));
    }
}
