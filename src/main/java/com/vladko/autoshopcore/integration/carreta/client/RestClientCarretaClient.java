package com.vladko.autoshopcore.integration.carreta.client;

import com.vladko.autoshopcore.integration.carreta.config.CarretaProperties;
import com.vladko.autoshopcore.integration.carreta.dto.CarretaOrderCreateRequest;
import com.vladko.autoshopcore.integration.carreta.dto.CarretaOrderResponse;
import com.vladko.autoshopcore.integration.carreta.dto.CarretaProfileResponse;
import com.vladko.autoshopcore.integration.carreta.dto.CarretaSearchResponse;
import com.vladko.autoshopcore.integration.shared.ExternalApiAuthenticationException;
import com.vladko.autoshopcore.integration.shared.ExternalApiConfigurationException;
import com.vladko.autoshopcore.integration.shared.ExternalApiContractException;
import com.vladko.autoshopcore.integration.shared.ExternalApiUnavailableException;
import com.vladko.autoshopcore.integration.shared.ExternalApiValidationException;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

@Component
public class RestClientCarretaClient implements CarretaClient {

    private final RestClient carretaRestClient;
    private final CarretaProperties properties;

    public RestClientCarretaClient(@Qualifier("carretaRestClient") RestClient carretaRestClient,
                                   CarretaProperties properties) {
        this.carretaRestClient = carretaRestClient;
        this.properties = properties;
    }

    @Override
    public CarretaSearchResponse search(String query) {
        requireConfigured();
        try {
            return carretaRestClient.get()
                    .uri(uriBuilder -> {
                        var builder = uriBuilder.path("/v1/search/")
                                .queryParam("api_key", properties.apiKey())
                                .queryParam("q", query);
                        if (StringUtils.hasText(properties.account())) {
                            builder.queryParam("account", properties.account());
                        }
                        return builder.build();
                    })
                    .retrieve()
                    .onStatus(HttpStatusCode::isError, (request, response) -> handleError(response.getStatusCode()))
                    .body(CarretaSearchResponse.class);
        } catch (RestClientException exception) {
            throw new ExternalApiUnavailableException("Carreta supplier provider is unavailable", exception);
        }
    }

    @Override
    public CarretaOrderResponse createOrder(CarretaOrderCreateRequest request, boolean testMode) {
        requireConfigured();
        try {
            return carretaRestClient.post()
                    .uri(uriBuilder -> {
                        var builder = uriBuilder.path("/v1/order/")
                                .queryParam("api_key", properties.apiKey());
                        if (testMode) {
                            builder.queryParam("test", "on");
                        }
                        return builder.build();
                    })
                    .body(request)
                    .retrieve()
                    .onStatus(HttpStatusCode::isError, (httpRequest, response) -> handleError(response.getStatusCode()))
                    .body(CarretaOrderResponse.class);
        } catch (RestClientException exception) {
            throw new ExternalApiUnavailableException("Carreta supplier provider is unavailable", exception);
        }
    }

    @Override
    public CarretaOrderResponse getOrder(Integer externalOrderId) {
        requireConfigured();
        try {
            return carretaRestClient.get()
                    .uri("/v1/order/{id}/?api_key={apiKey}", externalOrderId, properties.apiKey())
                    .retrieve()
                    .onStatus(HttpStatusCode::isError, (request, response) -> handleError(response.getStatusCode()))
                    .body(CarretaOrderResponse.class);
        } catch (RestClientException exception) {
            throw new ExternalApiUnavailableException("Carreta supplier provider is unavailable", exception);
        }
    }

    @Override
    public CarretaProfileResponse getProfile() {
        requireConfigured();
        try {
            return carretaRestClient.get()
                    .uri("/v1/profile/?api_key={apiKey}", properties.apiKey())
                    .retrieve()
                    .onStatus(HttpStatusCode::isError, (request, response) -> handleError(response.getStatusCode()))
                    .body(CarretaProfileResponse.class);
        } catch (RestClientException exception) {
            throw new ExternalApiUnavailableException("Carreta supplier provider is unavailable", exception);
        }
    }

    private void requireConfigured() {
        if (!StringUtils.hasText(properties.apiKey())) {
            throw new ExternalApiConfigurationException("Carreta API key is not configured");
        }
    }

    private void handleError(HttpStatusCode statusCode) {
        if (statusCode.value() == 401 || statusCode.value() == 403) {
            throw new ExternalApiAuthenticationException("Carreta authentication failed");
        }
        if (statusCode.value() == 400) {
            throw new ExternalApiValidationException("Carreta rejected request");
        }
        if (statusCode.is4xxClientError()) {
            throw new ExternalApiContractException("Carreta rejected request");
        }
        throw new ExternalApiUnavailableException("Carreta supplier provider is unavailable");
    }
}
