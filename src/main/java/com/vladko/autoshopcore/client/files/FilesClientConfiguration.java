package com.vladko.autoshopcore.client.files;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

@Configuration
@EnableConfigurationProperties(FilesServiceProperties.class)
public class FilesClientConfiguration {

    @Bean
    RestClient filesServiceRestClient(FilesServiceProperties properties) {
        return RestClient.builder()
                .baseUrl(properties.baseUrl())
                .build();
    }
}
