package com.vladko.autoshopcore.client.controller;

import com.vladko.autoshopcore.client.dto.CustomerVehicleCreateDTO;
import com.vladko.autoshopcore.client.dto.CustomerVehicleUpdateDTO;
import com.vladko.autoshopcore.client.service.CustomerVehicleSelfService;
import com.vladko.autoshopcore.vehicle.dto.VehicleResponseDTO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/customers/me/vehicles")
@RequiredArgsConstructor
public class CustomerVehicleSelfServiceController {

    private final CustomerVehicleSelfService customerVehicleSelfService;

    @GetMapping
    public ResponseEntity<List<VehicleResponseDTO>> getCurrentCustomerVehicles() {
        return ResponseEntity.ok(customerVehicleSelfService.getCurrentCustomerVehicles());
    }

    @GetMapping("/{vehicleId}")
    public ResponseEntity<VehicleResponseDTO> getCurrentCustomerVehicle(@PathVariable Integer vehicleId) {
        return ResponseEntity.ok(customerVehicleSelfService.getCurrentCustomerVehicle(vehicleId));
    }

    @PostMapping
    public ResponseEntity<VehicleResponseDTO> createVehicle(@Valid @RequestBody CustomerVehicleCreateDTO dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(customerVehicleSelfService.createVehicle(dto));
    }

    @PutMapping("/{vehicleId}")
    public ResponseEntity<VehicleResponseDTO> updateVehicle(@PathVariable Integer vehicleId,
                                                            @Valid @RequestBody CustomerVehicleUpdateDTO dto) {
        return ResponseEntity.ok(customerVehicleSelfService.updateVehicle(vehicleId, dto));
    }

    @DeleteMapping("/{vehicleId}")
    public ResponseEntity<Void> deleteVehicle(@PathVariable Integer vehicleId) {
        customerVehicleSelfService.deleteVehicle(vehicleId);
        return ResponseEntity.noContent().build();
    }
}
