package com.vladko.autoshopcore.parts.controller;

import com.vladko.autoshopcore.parts.dto.vehicle.VehicleScopedPartSearchResponseDTO;
import com.vladko.autoshopcore.parts.service.vehicle.VehicleScopedPartSearchService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/orders/{orderId}/parts/search-by-name")
@RequiredArgsConstructor
public class OrderVehicleScopedPartSearchController {

    private final VehicleScopedPartSearchService vehicleScopedPartSearchService;

    @GetMapping
    public ResponseEntity<VehicleScopedPartSearchResponseDTO> search(@PathVariable Integer orderId,
                                                                     @RequestParam String query,
                                                                     @RequestParam(required = false) Boolean availableOnly,
                                                                     @RequestParam(required = false) Integer limit,
                                                                     @RequestParam(required = false) Integer offset) {
        return ResponseEntity.ok(vehicleScopedPartSearchService.searchByName(orderId, query, availableOnly, limit, offset));
    }
}
