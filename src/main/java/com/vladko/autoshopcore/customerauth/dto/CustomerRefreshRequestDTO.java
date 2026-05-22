package com.vladko.autoshopcore.customerauth.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class CustomerRefreshRequestDTO {
    @NotBlank
    private String refreshToken;
}
