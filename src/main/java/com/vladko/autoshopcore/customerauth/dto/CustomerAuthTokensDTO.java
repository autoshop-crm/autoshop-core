package com.vladko.autoshopcore.customerauth.dto;

import lombok.Builder;
import lombok.Value;
import lombok.extern.jackson.Jacksonized;

import java.time.Instant;
import java.util.Set;

@Value
@Builder
@Jacksonized
public class CustomerAuthTokensDTO {
    Long authUserId;
    String email;
    String phoneNumber;
    String firstName;
    String lastName;
    Set<String> roles;
    String accessToken;
    String refreshToken;
    Instant expiresAt;
    boolean emailVerified;
    String accountStatus;
}
