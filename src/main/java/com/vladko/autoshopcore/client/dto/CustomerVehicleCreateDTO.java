package com.vladko.autoshopcore.client.dto;

import jakarta.validation.constraints.NotBlank;
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
public class CustomerVehicleCreateDTO {
    @NotBlank
    @Size(min = 1, max = 25)
    private String brand;

    @NotBlank
    @Size(min = 1, max = 25)
    private String model;

    @NotBlank
    @Pattern(regexp = "[A-HJ-NPR-Z0-9]{17}", flags = Pattern.Flag.CASE_INSENSITIVE)
    private String vin;

    @NotBlank
    @Pattern(regexp = "^[A-Z0-9-]{4,12}$", flags = Pattern.Flag.CASE_INSENSITIVE)
    private String licensePlate;
}
