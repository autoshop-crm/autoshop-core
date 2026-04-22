package com.vladko.autoshopcore.integration.carreta.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

@ConfigurationProperties(prefix = "app.carreta")
public record CarretaProperties(
        String baseUrl,
        String apiKey,
        String account,
        boolean testOrdersEnabled,
        Duration connectTimeout,
        Duration readTimeout,
        Cache cache,
        Retry retry
) {

    public record Cache(Duration searchTtl) {
    }

    public record Retry(int maxAttempts, Duration backoff) {
    }
}
