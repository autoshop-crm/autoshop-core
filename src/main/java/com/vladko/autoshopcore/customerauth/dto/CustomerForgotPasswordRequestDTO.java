package com.vladko.autoshopcore.customerauth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class CustomerForgotPasswordRequestDTO {
    @NotBlank
    @Email
    @Size(max = 50)
    private String email;
}
