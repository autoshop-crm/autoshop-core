package com.vladko.autoshopcore.parts.controller;

import com.vladko.autoshopcore.parts.dto.OrderPartsOverviewResponseDTO;
import com.vladko.autoshopcore.parts.service.OrderPartsOverviewService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/orders/{orderId}/parts/overview")
@RequiredArgsConstructor
public class OrderPartsOverviewController {

    private final OrderPartsOverviewService orderPartsOverviewService;

    @GetMapping
    public ResponseEntity<OrderPartsOverviewResponseDTO> getOverview(@PathVariable Integer orderId) {
        return ResponseEntity.ok(orderPartsOverviewService.getOverview(orderId));
    }
}
