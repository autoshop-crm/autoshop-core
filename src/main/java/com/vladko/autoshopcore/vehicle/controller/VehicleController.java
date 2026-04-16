package com.vladko.autoshopcore.vehicle.controller;

import com.vladko.autoshopcore.vehicle.dto.VehicleCreateDTO;
import com.vladko.autoshopcore.vehicle.dto.VehicleResponseDTO;
import com.vladko.autoshopcore.vehicle.dto.VehicleUpdateDTO;
import com.vladko.autoshopcore.vehicle.service.VehicleService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/vehicles")
@RequiredArgsConstructor
public class VehicleController {

    private final VehicleService vehicleService;

    @PostMapping
    public ResponseEntity<VehicleResponseDTO> create(@Valid @RequestBody VehicleCreateDTO dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(vehicleService.create(dto));
    }

    @GetMapping("/{id}")
    public ResponseEntity<VehicleResponseDTO> getById(@PathVariable Integer id) {
        return ResponseEntity.ok(vehicleService.getById(id));
    }

    @GetMapping("/vin/{vin}")
    public ResponseEntity<VehicleResponseDTO> getByVin(@PathVariable String vin) {
        return ResponseEntity.ok(vehicleService.getByVin(vin));
    }

    @GetMapping("/customer/{customerId}")
    public ResponseEntity<List<VehicleResponseDTO>> getAllByCustomerId(@PathVariable Integer customerId) {
        return ResponseEntity.ok(vehicleService.getAllByCustomerId(customerId));
    }

    @PutMapping("/{id}")
    public ResponseEntity<VehicleResponseDTO> update(@PathVariable Integer id,
                                                     @Valid @RequestBody VehicleUpdateDTO dto) {
        return ResponseEntity.ok(vehicleService.update(id, dto));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Integer id) {
        vehicleService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
