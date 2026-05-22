package com.vladko.autoshopcore.customerauth.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class CustomerLogoutRequestDTO {
    @NotBlank
    private String refreshToken;
}
