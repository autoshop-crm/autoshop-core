package com.vladko.autoshopcore.procurement.controller;

import com.vladko.autoshopcore.parts.dto.PartResponseDTO;
import com.vladko.autoshopcore.procurement.dto.StockReceiptDTO;
import com.vladko.autoshopcore.procurement.service.StockReceivingService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/procurement/stock-receipts")
@RequiredArgsConstructor
public class StockReceiptController {

    private final StockReceivingService stockReceivingService;

    @PostMapping
    public ResponseEntity<PartResponseDTO> receive(@Valid @RequestBody StockReceiptDTO dto) {
        return ResponseEntity.ok(stockReceivingService.receive(dto));
    }
}
