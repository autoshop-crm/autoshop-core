package com.vladko.autoshopcore.security;

import java.time.Instant;
import java.util.Set;

public record AuthenticatedUser(
        Long userId,
        String email,
        Set<String> roles,
        String jti,
        Instant expiresAt
) {
}
