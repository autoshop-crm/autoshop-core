package com.vladko.autoshopcore.customerauth.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class CustomerVerifyEmailRequestDTO {
    @NotBlank
    private String verificationToken;
}
