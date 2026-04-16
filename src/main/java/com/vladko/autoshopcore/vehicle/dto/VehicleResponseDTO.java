package com.vladko.autoshopcore.vehicle.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Builder
@AllArgsConstructor
@NoArgsConstructor
@Data
public class VehicleResponseDTO {
    private Integer id;
    private Integer customerId;
    private String brand;
    private String model;
    private String vin;
    private String licensePlate;
    private Instant createdAt;
    private Instant updatedAt;
}
