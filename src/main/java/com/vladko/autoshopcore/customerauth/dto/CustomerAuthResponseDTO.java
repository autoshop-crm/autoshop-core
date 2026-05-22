package com.vladko.autoshopcore.customerauth.dto;

import lombok.Builder;
import lombok.Value;

import java.time.Instant;
import java.util.Set;

@Value
@Builder
public class CustomerAuthResponseDTO {
    Integer customerId;
    Long authUserId;
    String email;
    String phoneNumber;
    Set<String> roles;
    String accessToken;
    String refreshToken;
    Instant expiresAt;
    boolean emailVerified;
    boolean profileCompleted;
    boolean requiresEmailVerification;
}
