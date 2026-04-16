package com.vladko.autoshopcore.vehicle.dto;

import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Builder
@AllArgsConstructor
@NoArgsConstructor
@Data
public class VehicleUpdateDTO {
    @Pattern(regexp = "^(?!\\s*$).{1,25}$")
    private String brand;

    @Pattern(regexp = "^(?!\\s*$).{1,25}$")
    private String model;

    @Pattern(regexp = "[A-HJ-NPR-Z0-9]{17}", flags = Pattern.Flag.CASE_INSENSITIVE)
    private String vin;

    @Pattern(regexp = "^[A-Z0-9-]{4,12}$", flags = Pattern.Flag.CASE_INSENSITIVE)
    private String licensePlate;
}
