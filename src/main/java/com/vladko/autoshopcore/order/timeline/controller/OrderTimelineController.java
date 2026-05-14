package com.vladko.autoshopcore.order.timeline.controller;

import com.vladko.autoshopcore.order.timeline.dto.OrderTimelineEntryResponseDTO;
import com.vladko.autoshopcore.order.timeline.service.OrderTimelineService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/orders/{orderId}/timeline")
@RequiredArgsConstructor
public class OrderTimelineController {

    private final OrderTimelineService orderTimelineService;

    @GetMapping
    public ResponseEntity<List<OrderTimelineEntryResponseDTO>> getStaffTimeline(@PathVariable Integer orderId) {
        return ResponseEntity.ok(orderTimelineService.getStaffTimeline(orderId));
    }

    @GetMapping("/customer")
    public ResponseEntity<List<OrderTimelineEntryResponseDTO>> getCustomerTimeline(@PathVariable Integer orderId) {
        return ResponseEntity.ok(orderTimelineService.getCustomerTimeline(orderId));
    }
}
