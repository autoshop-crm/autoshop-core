package com.vladko.autoshopcore.order.query.controller;

import com.vladko.autoshopcore.order.entity.OrderStatus;
import com.vladko.autoshopcore.order.query.dto.OrderQueueSummaryDTO;
import com.vladko.autoshopcore.order.query.dto.OrderSearchResponseDTO;
import com.vladko.autoshopcore.order.query.service.OrderQueryService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;

@RestController
@RequestMapping("/api/crm/orders")
@RequiredArgsConstructor
public class OrderQueryController {

    private final OrderQueryService orderQueryService;

    @GetMapping("/search")
    public ResponseEntity<OrderSearchResponseDTO> search(@RequestParam(required = false) Integer customerId,
                                                         @RequestParam(required = false) Integer vehicleId,
                                                         @RequestParam(required = false) OrderStatus status,
                                                         @RequestParam(required = false) Integer employeeId,
                                                         @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant plannedFrom,
                                                         @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant plannedTo,
                                                         @RequestParam(required = false) String q,
                                                         @RequestParam(defaultValue = "0") int page,
                                                         @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(orderQueryService.search(customerId, vehicleId, status, employeeId, plannedFrom, plannedTo, q, page, size));
    }

    @GetMapping("/queue-summary")
    public ResponseEntity<OrderQueueSummaryDTO> queueSummary() {
        return ResponseEntity.ok(orderQueryService.queueSummary());
    }
}
