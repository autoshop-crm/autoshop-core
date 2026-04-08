package com.vladko.autoshopcore.client.dto;

import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Builder
@AllArgsConstructor
@NoArgsConstructor
@Data
public class CustomerCreateDTO {
    @NotNull
    @NotBlank
    @Size(min = 2, max = 64)
    private String firstName;

    @NotNull
    @NotBlank
    @Size(min = 2, max = 64)
    private String lastName;

    @NotNull
    @NotBlank
    @Pattern(regexp = "^\\+?[0-9]{10,15}$")
    private String phoneNumber;

    @NotNull
    @NotBlank
    @Email
    private String email;
}
