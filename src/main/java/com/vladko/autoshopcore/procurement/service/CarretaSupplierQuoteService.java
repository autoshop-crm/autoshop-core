package com.vladko.autoshopcore.procurement.service;

import com.vladko.autoshopcore.integration.carreta.client.CarretaClient;
import com.vladko.autoshopcore.integration.carreta.config.CarretaProperties;
import com.vladko.autoshopcore.integration.carreta.dto.CarretaSearchResponse;
import com.vladko.autoshopcore.integration.carreta.mapper.CarretaQuoteMapper;
import com.vladko.autoshopcore.integration.carreta.support.CarretaCacheKeyFactory;
import com.vladko.autoshopcore.integration.shared.ExternalApiRetryExecutor;
import com.vladko.autoshopcore.integration.shared.ExternalApiUnavailableException;
import com.vladko.autoshopcore.integration.shared.JsonRedisCacheService;
import com.vladko.autoshopcore.procurement.dto.SupplierQuoteSearchResponseDTO;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import java.util.Locale;

@Service
public class CarretaSupplierQuoteService implements SupplierQuoteService {

    private final CarretaClient carretaClient;
    private final CarretaProperties properties;
    private final ExternalApiRetryExecutor retryExecutor;
    private final JsonRedisCacheService cacheService;
    private final CarretaCacheKeyFactory cacheKeyFactory;
    private final CarretaQuoteMapper mapper;

    public CarretaSupplierQuoteService(CarretaClient carretaClient,
                                       CarretaProperties properties,
                                       @Qualifier("carretaRetryExecutor") ExternalApiRetryExecutor retryExecutor,
                                       JsonRedisCacheService cacheService,
                                       CarretaCacheKeyFactory cacheKeyFactory,
                                       CarretaQuoteMapper mapper) {
        this.carretaClient = carretaClient;
        this.properties = properties;
        this.retryExecutor = retryExecutor;
        this.cacheService = cacheService;
        this.cacheKeyFactory = cacheKeyFactory;
        this.mapper = mapper;
    }

    @Override
    public SupplierQuoteSearchResponseDTO searchCarretaQuotes(String query) {
        String normalizedQuery = normalizeQuery(query);
        String cacheKey = cacheKeyFactory.searchKey(properties.account(), normalizedQuery);

        return cacheService.get(cacheKey, SupplierQuoteSearchResponseDTO.class)
                .map(this::markCached)
                .orElseGet(() -> fetchAndCache(cacheKey, normalizedQuery));
    }

    private SupplierQuoteSearchResponseDTO fetchAndCache(String cacheKey, String query) {
        try {
            CarretaSearchResponse response = retryExecutor.execute(() -> carretaClient.search(query));
            Instant fetchedAt = Instant.now();
            SupplierQuoteSearchResponseDTO dto = SupplierQuoteSearchResponseDTO.builder()
                    .query(query)
                    .provider("CARRETA")
                    .cached(false)
                    .fallback(false)
                    .cachedAt(fetchedAt)
                    .cacheExpiresAt(fetchedAt.plus(properties.cache().searchTtl()))
                    .quotes(response == null || response.getObjects() == null
                            ? List.of()
                            : response.getObjects().stream()
                            .map(item -> mapper.map(query, item, fetchedAt))
                            .toList())
                    .build();
            cacheService.put(cacheKey, dto, properties.cache().searchTtl());
            return dto;
        } catch (ExternalApiUnavailableException exception) {
            return cacheService.get(cacheKey, SupplierQuoteSearchResponseDTO.class)
                    .map(this::markFallback)
                    .orElseThrow(() -> exception);
        }
    }

    private SupplierQuoteSearchResponseDTO markCached(SupplierQuoteSearchResponseDTO dto) {
        dto.setCached(true);
        dto.setFallback(false);
        return dto;
    }

    private SupplierQuoteSearchResponseDTO markFallback(SupplierQuoteSearchResponseDTO dto) {
        dto.setCached(true);
        dto.setFallback(true);
        return dto;
    }

    private String normalizeQuery(String query) {
        if (query == null || query.trim().isEmpty()) {
            throw new IllegalArgumentException("Supplier search query must not be blank");
        }
        return query.trim().toUpperCase(Locale.ROOT);
    }
}
