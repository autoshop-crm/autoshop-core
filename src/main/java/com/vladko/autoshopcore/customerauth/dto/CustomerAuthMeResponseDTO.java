package com.vladko.autoshopcore.customerauth.dto;

import com.vladko.autoshopcore.client.dto.CustomerResponseDTO;
import lombok.Builder;
import lombok.Value;

import java.util.Set;

@Value
@Builder
public class CustomerAuthMeResponseDTO {
    boolean authenticated;
    Long authUserId;
    String email;
    Set<String> roles;
    boolean emailVerified;
    CustomerResponseDTO customer;
    boolean profileCompleted;
}
