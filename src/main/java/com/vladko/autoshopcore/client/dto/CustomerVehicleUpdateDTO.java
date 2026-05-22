package com.vladko.autoshopcore.client.dto;

import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Builder
@AllArgsConstructor
@NoArgsConstructor
@Data
public class CustomerVehicleUpdateDTO {
    @Pattern(regexp = "^(?!\\s*$).{1,25}$")
    private String brand;

    @Pattern(regexp = "^(?!\\s*$).{1,25}$")
    private String model;

    @Pattern(regexp = "[A-HJ-NPR-Z0-9]{17}", flags = Pattern.Flag.CASE_INSENSITIVE)
    private String vin;

    @Pattern(regexp = "^[A-Z0-9-]{4,12}$", flags = Pattern.Flag.CASE_INSENSITIVE)
    private String licensePlate;
}
