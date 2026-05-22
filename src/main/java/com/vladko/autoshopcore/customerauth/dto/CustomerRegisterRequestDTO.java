package com.vladko.autoshopcore.customerauth.dto;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class CustomerRegisterRequestDTO {
    @NotBlank
    @Email
    @Size(max = 50)
    private String email;

    @NotBlank
    @Pattern(regexp = "^\\+?[0-9]{10,15}$")
    private String phoneNumber;

    @NotBlank
    @Size(min = 8, max = 100)
    private String password;

    @NotBlank
    @Size(min = 2, max = 50)
    private String firstName;

    @NotBlank
    @Size(min = 2, max = 50)
    private String lastName;

    @AssertTrue
    private boolean acceptTerms;

    @AssertTrue
    private boolean acceptPrivacyPolicy;
}
