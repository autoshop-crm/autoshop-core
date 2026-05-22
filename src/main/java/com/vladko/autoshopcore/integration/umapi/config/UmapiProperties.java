package com.vladko.autoshopcore.integration.umapi.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

@ConfigurationProperties(prefix = "app.umapi")
public record UmapiProperties(
        String baseUrl,
        String apiKey,
        String languageCode,
        String regionCode,
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
