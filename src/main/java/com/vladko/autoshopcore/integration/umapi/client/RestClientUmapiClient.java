package com.vladko.autoshopcore.integration.umapi.client;

import com.vladko.autoshopcore.integration.shared.ExternalApiAuthenticationException;
import com.vladko.autoshopcore.integration.shared.ExternalApiConfigurationException;
import com.vladko.autoshopcore.integration.shared.ExternalApiContractException;
import com.vladko.autoshopcore.integration.shared.ExternalApiUnavailableException;
import com.vladko.autoshopcore.integration.umapi.config.UmapiProperties;
import com.vladko.autoshopcore.integration.umapi.dto.UmapiAnalogsResponse;
import com.vladko.autoshopcore.integration.umapi.dto.UmapiArticleItem;
import com.vladko.autoshopcore.integration.umapi.dto.UmapiBrandRefinementItem;
import com.vladko.autoshopcore.integration.umapi.dto.catalog.UmapiCatalogArticlesResponse;
import com.vladko.autoshopcore.integration.umapi.dto.catalog.UmapiFuseProductGroupResponse;
import com.vladko.autoshopcore.integration.umapi.dto.catalog.UmapiManufacturerResponse;
import com.vladko.autoshopcore.integration.umapi.dto.catalog.UmapiModelSeriesResponse;
import com.vladko.autoshopcore.integration.umapi.dto.catalog.UmapiPassengerModificationResponse;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.util.List;
import java.util.stream.Collectors;

@Component
public class RestClientUmapiClient implements UmapiClient {

    private static final ParameterizedTypeReference<List<UmapiBrandRefinementItem>> BRAND_REFINEMENT_TYPE =
            new ParameterizedTypeReference<>() {
            };
    private static final ParameterizedTypeReference<List<UmapiManufacturerResponse>> MANUFACTURERS_TYPE =
            new ParameterizedTypeReference<>() {
            };
    private static final ParameterizedTypeReference<List<UmapiModelSeriesResponse>> MODEL_SERIES_TYPE =
            new ParameterizedTypeReference<>() {
            };
    private static final ParameterizedTypeReference<List<UmapiPassengerModificationResponse>> PASSENGER_MODIFICATIONS_TYPE =
            new ParameterizedTypeReference<>() {
            };
    private static final ParameterizedTypeReference<List<UmapiFuseProductGroupResponse>> FUSE_PRODUCT_GROUPS_TYPE =
            new ParameterizedTypeReference<>() {
            };

    private final RestClient umapiRestClient;
    private final UmapiProperties properties;

    public RestClientUmapiClient(@Qualifier("umapiRestClient") RestClient umapiRestClient,
                                 UmapiProperties properties) {
        this.umapiRestClient = umapiRestClient;
        this.properties = properties;
    }

    @Override
    public List<UmapiBrandRefinementItem> refineBrand(String articleNumber) {
        requireConfigured();
        try {
            return umapiRestClient.get()
                    .uri("/v2/autocatalog/{language}-{region}/BrandRefinement/{article}",
                            properties.languageCode(), properties.regionCode(), articleNumber)
                    .header("X-App-Key", properties.apiKey())
                    .retrieve()
                    .onStatus(HttpStatusCode::isError, (request, response) -> handleError(response.getStatusCode()))
                    .body(BRAND_REFINEMENT_TYPE);
        } catch (RestClientException exception) {
            throw new ExternalApiUnavailableException("UMAPI catalog is unavailable", exception);
        }
    }

    @Override
    public UmapiAnalogsResponse findAnalogs(String articleNumber, String brand, int limit, int offset) {
        requireConfigured();
        try {
            return umapiRestClient.get()
                    .uri(uriBuilder -> {
                        String path = StringUtils.hasText(brand)
                                ? "/v2/autocatalog/{language}-{region}/Analogs/{article}/{brand}"
                                : "/v2/autocatalog/{language}-{region}/Analogs/{article}";
                        var builder = uriBuilder.path(path)
                                .queryParam("limit", limit)
                                .queryParam("offset", offset);
                        return StringUtils.hasText(brand)
                                ? builder.build(properties.languageCode(), properties.regionCode(), articleNumber, brand)
                                : builder.build(properties.languageCode(), properties.regionCode(), articleNumber);
                    })
                    .header("X-App-Key", properties.apiKey())
                    .retrieve()
                    .onStatus(HttpStatusCode::isError, (request, response) -> handleError(response.getStatusCode()))
                    .body(UmapiAnalogsResponse.class);
        } catch (RestClientException exception) {
            throw new ExternalApiUnavailableException("UMAPI catalog is unavailable", exception);
        }
    }

    @Override
    public UmapiArticleItem getArticle(Integer articleId) {
        requireConfigured();
        try {
            return umapiRestClient.get()
                    .uri("/v2/autocatalog/{language}-{region}/Article/{id}",
                            properties.languageCode(), properties.regionCode(), articleId)
                    .header("X-App-Key", properties.apiKey())
                    .retrieve()
                    .onStatus(HttpStatusCode::isError, (request, response) -> handleError(response.getStatusCode()))
                    .body(UmapiArticleItem.class);
        } catch (RestClientException exception) {
            throw new ExternalApiUnavailableException("UMAPI catalog is unavailable", exception);
        }
    }

    @Override
    public List<UmapiManufacturerResponse> getManufacturers(String type, boolean popular) {
        requireConfigured();
        try {
            return umapiRestClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .path(catalogPath("Manufacturers"))
                            .queryParam("type", type)
                            .queryParam("popular", popular)
                            .build(properties.languageCode(), properties.regionCode()))
                    .header("X-App-Key", properties.apiKey())
                    .retrieve()
                    .onStatus(HttpStatusCode::isError, (request, response) -> handleError(response.getStatusCode()))
                    .body(MANUFACTURERS_TYPE);
        } catch (RestClientException exception) {
            throw new ExternalApiUnavailableException("UMAPI catalog is unavailable", exception);
        }
    }

    @Override
    public List<UmapiModelSeriesResponse> getModelSeries(String type, Integer manufacturerId) {
        requireConfigured();
        try {
            return umapiRestClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .path(catalogPath("ModelSeries"))
                            .queryParam("type", type)
                            .queryParam("MFA_ID", manufacturerId)
                            .build(properties.languageCode(), properties.regionCode()))
                    .header("X-App-Key", properties.apiKey())
                    .retrieve()
                    .onStatus(HttpStatusCode::isError, (request, response) -> handleError(response.getStatusCode()))
                    .body(MODEL_SERIES_TYPE);
        } catch (RestClientException exception) {
            throw new ExternalApiUnavailableException("UMAPI catalog is unavailable", exception);
        }
    }

    @Override
    public List<UmapiPassengerModificationResponse> getPassengerModifications(String type, Integer modelSeriesId) {
        requireConfigured();
        try {
            return umapiRestClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .path(catalogPath("Passangers"))
                            .queryParam("type", type)
                            .queryParam("MS_ID", modelSeriesId)
                            .build(properties.languageCode(), properties.regionCode()))
                    .header("X-App-Key", properties.apiKey())
                    .retrieve()
                    .onStatus(HttpStatusCode::isError, (request, response) -> handleError(response.getStatusCode()))
                    .body(PASSENGER_MODIFICATIONS_TYPE);
        } catch (RestClientException exception) {
            throw new ExternalApiUnavailableException("UMAPI catalog is unavailable", exception);
        }
    }

    @Override
    public List<UmapiFuseProductGroupResponse> getFuseProductGroups(String type, Integer modificationId) {
        requireConfigured();
        try {
            return umapiRestClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .path(catalogPath("Fuse"))
                            .queryParam("type", type)
                            .queryParam("ID", modificationId)
                            .build(properties.languageCode(), properties.regionCode()))
                    .header("X-App-Key", properties.apiKey())
                    .retrieve()
                    .onStatus(HttpStatusCode::isError, (request, response) -> handleError(response.getStatusCode()))
                    .body(FUSE_PRODUCT_GROUPS_TYPE);
        } catch (RestClientException exception) {
            throw new ExternalApiUnavailableException("UMAPI catalog is unavailable", exception);
        }
    }

    @Override
    public List<UmapiArticleItem> getArticles(String type,
                                              List<Integer> productGroupIds,
                                              Integer modificationId,
                                              Integer supplierId,
                                              int limit,
                                              int offset) {
        requireConfigured();
        try {
            UmapiCatalogArticlesResponse response = umapiRestClient.get()
                    .uri(uriBuilder -> {
                        var builder = uriBuilder
                                .path(catalogPath("Articles"))
                                .queryParam("type", type)
                                .queryParam("PT_IDS", joinIds(productGroupIds))
                                .queryParam("ID", modificationId)
                                .queryParam("limit", limit)
                                .queryParam("offset", offset);
                        if (supplierId != null) {
                            builder.queryParam("SUP_ID", supplierId);
                        }
                        return builder.build(properties.languageCode(), properties.regionCode());
                    })
                    .header("X-App-Key", properties.apiKey())
                    .retrieve()
                    .onStatus(HttpStatusCode::isError, (request, clientResponse) -> handleError(clientResponse.getStatusCode()))
                    .body(UmapiCatalogArticlesResponse.class);
            return response == null || response.getData() == null ? List.of() : response.getData();
        } catch (RestClientException exception) {
            throw new ExternalApiUnavailableException("UMAPI catalog is unavailable", exception);
        }
    }

    private void requireConfigured() {
        if (!StringUtils.hasText(properties.apiKey())) {
            throw new ExternalApiConfigurationException("UMAPI API key is not configured");
        }
    }

    private String catalogPath(String suffix) {
        return "/v2/autocatalog/{language}-{region}/" + suffix;
    }

    private String joinIds(List<Integer> ids) {
        return ids.stream()
                .map(String::valueOf)
                .collect(Collectors.joining(","));
    }

    private void handleError(HttpStatusCode statusCode) {
        if (statusCode.value() == 401 || statusCode.value() == 403) {
            throw new ExternalApiAuthenticationException("UMAPI authentication failed");
        }
        if (statusCode.value() == 402) {
            throw new ExternalApiAuthenticationException("UMAPI subscription/payment is required");
        }
        if (statusCode.is4xxClientError()) {
            throw new ExternalApiContractException("UMAPI rejected request");
        }
        throw new ExternalApiUnavailableException("UMAPI catalog is unavailable");
    }
}
