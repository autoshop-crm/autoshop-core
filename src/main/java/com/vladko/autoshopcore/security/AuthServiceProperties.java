package com.vladko.autoshopcore.security;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

@ConfigurationProperties(prefix = "app.auth")
public record AuthServiceProperties(
        String baseUrl,
        String validatePath,
        String customerRegisterPath,
        String customerLoginPath,
        String customerRefreshPath,
        String customerLogoutPath,
        String customerForgotPasswordPath,
        String customerResetPasswordPath,
        String customerVerifyEmailPath,
        Duration connectTimeout,
        Duration readTimeout,
        boolean enabled
) {
}
