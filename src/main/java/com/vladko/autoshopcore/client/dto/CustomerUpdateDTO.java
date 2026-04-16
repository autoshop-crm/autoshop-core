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
public class CustomerUpdateDTO {
    @Pattern(regexp = "^(?!\\s*$).{2,50}$")
    private String firstName;

    @Pattern(regexp = "^(?!\\s*$).{2,50}$")
    private String lastName;

    @Pattern(regexp = "^\\+?[0-9]{10,15}$")
    private String phoneNumber;

    @Email
    @Size(max = 50)
    private String email;
}
