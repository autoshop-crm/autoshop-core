package com.vladko.autoshopcore.integration.carreta.config;

import com.vladko.autoshopcore.integration.shared.ExternalApiRetryExecutor;
import org.springframework.boot.web.client.ClientHttpRequestFactories;
import org.springframework.boot.web.client.ClientHttpRequestFactorySettings;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

@Configuration
public class CarretaClientConfiguration {

    @Bean
    RestClient carretaRestClient(CarretaProperties properties) {
        ClientHttpRequestFactorySettings settings = ClientHttpRequestFactorySettings.DEFAULTS
                .withConnectTimeout(properties.connectTimeout())
                .withReadTimeout(properties.readTimeout());

        return RestClient.builder()
                .baseUrl(properties.baseUrl())
                .requestFactory(ClientHttpRequestFactories.get(settings))
                .build();
    }

    @Bean
    ExternalApiRetryExecutor carretaRetryExecutor(CarretaProperties properties) {
        return new ExternalApiRetryExecutor(properties.retry().maxAttempts(), properties.retry().backoff());
    }
}
