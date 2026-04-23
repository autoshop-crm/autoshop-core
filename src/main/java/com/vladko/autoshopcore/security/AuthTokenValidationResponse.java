package com.vladko.autoshopcore.security;

import java.time.Instant;
import java.util.Set;

public record AuthTokenValidationResponse(
        boolean valid,
        Long userId,
        String email,
        Set<String> roles,
        String tokenType,
        String jti,
        Instant expiresAt,
        String message
) {
}
