package com.vladko.autoshopcore.procurement.controller;

import com.vladko.autoshopcore.procurement.dto.SupplierQuoteSearchResponseDTO;
import com.vladko.autoshopcore.procurement.service.SupplierQuoteService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/procurement/supplier-quotes")
@RequiredArgsConstructor
public class SupplierQuoteController {

    private final SupplierQuoteService supplierQuoteService;

    @GetMapping("/search")
    public ResponseEntity<SupplierQuoteSearchResponseDTO> search(@RequestParam String query) {
        return ResponseEntity.ok(supplierQuoteService.searchCarretaQuotes(query));
    }
}
