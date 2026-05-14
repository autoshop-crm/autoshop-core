package com.vladko.autoshopcore.order.controller;

import com.vladko.autoshopcore.order.dto.OrderResponseDTO;
import com.vladko.autoshopcore.order.entity.LegacyOrderStatus;
import com.vladko.autoshopcore.order.service.LegacyOrderCompatibilityService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/orders/legacy")
@RequiredArgsConstructor
public class LegacyOrderController {

    private final LegacyOrderCompatibilityService legacyOrderCompatibilityService;

    @GetMapping("/{id}")
    public ResponseEntity<OrderResponseDTO> getById(@PathVariable Integer id) {
        return ResponseEntity.ok(legacyOrderCompatibilityService.getById(id));
    }

    @GetMapping("/customer/{customerId}")
    public ResponseEntity<List<OrderResponseDTO>> getAllByCustomerId(@PathVariable Integer customerId) {
        return ResponseEntity.ok(legacyOrderCompatibilityService.getAllByCustomerId(customerId));
    }

    @GetMapping("/vehicle/{vehicleId}")
    public ResponseEntity<List<OrderResponseDTO>> getAllByVehicleId(@PathVariable Integer vehicleId) {
        return ResponseEntity.ok(legacyOrderCompatibilityService.getAllByVehicleId(vehicleId));
    }

    @GetMapping("/status/{status}")
    public ResponseEntity<List<OrderResponseDTO>> getAllByStatus(@PathVariable LegacyOrderStatus status) {
        return ResponseEntity.ok(legacyOrderCompatibilityService.getAllByStatus(status));
    }
}
