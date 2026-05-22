package com.vladko.autoshopcore.parts.controller;

import com.vladko.autoshopcore.parts.service.OrderRequestedPartQuoteService;
import com.vladko.autoshopcore.procurement.dto.SupplierQuoteSearchResponseDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/orders/{orderId}/requested-parts/{requestedPartId}/quotes")
@RequiredArgsConstructor
public class OrderRequestedPartQuoteController {

    private final OrderRequestedPartQuoteService orderRequestedPartQuoteService;

    @GetMapping
    public ResponseEntity<SupplierQuoteSearchResponseDTO> getQuotes(@PathVariable Integer orderId,
                                                                    @PathVariable Integer requestedPartId) {
        return ResponseEntity.ok(orderRequestedPartQuoteService.getQuotes(orderId, requestedPartId));
    }
}
