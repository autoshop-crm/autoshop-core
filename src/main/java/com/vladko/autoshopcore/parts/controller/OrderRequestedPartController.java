package com.vladko.autoshopcore.parts.controller;

import com.vladko.autoshopcore.parts.dto.OrderRequestedPartCreateDTO;
import com.vladko.autoshopcore.parts.dto.OrderRequestedPartResponseDTO;
import com.vladko.autoshopcore.parts.service.OrderRequestedPartService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/orders/{orderId}/requested-parts")
@RequiredArgsConstructor
public class OrderRequestedPartController {

    private final OrderRequestedPartService orderRequestedPartService;

    @PostMapping
    public ResponseEntity<OrderRequestedPartResponseDTO> create(@PathVariable Integer orderId,
                                                                @Valid @RequestBody OrderRequestedPartCreateDTO dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(orderRequestedPartService.create(orderId, dto));
    }

    @GetMapping
    public ResponseEntity<List<OrderRequestedPartResponseDTO>> getAll(@PathVariable Integer orderId) {
        return ResponseEntity.ok(orderRequestedPartService.getAllByOrderId(orderId));
    }
}
