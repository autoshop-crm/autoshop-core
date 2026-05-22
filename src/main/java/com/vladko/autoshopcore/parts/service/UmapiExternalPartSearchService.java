package com.vladko.autoshopcore.parts.service;

import com.vladko.autoshopcore.integration.shared.ExternalApiRetryExecutor;
import com.vladko.autoshopcore.integration.shared.ExternalApiUnavailableException;
import com.vladko.autoshopcore.integration.shared.JsonRedisCacheService;
import com.vladko.autoshopcore.integration.umapi.client.UmapiClient;
import com.vladko.autoshopcore.integration.umapi.config.UmapiProperties;
import com.vladko.autoshopcore.integration.umapi.dto.UmapiAnalogsResponse;
import com.vladko.autoshopcore.integration.umapi.mapper.UmapiCatalogItemMapper;
import com.vladko.autoshopcore.integration.umapi.support.UmapiArticleNormalizer;
import com.vladko.autoshopcore.integration.umapi.support.UmapiCacheKeyFactory;
import com.vladko.autoshopcore.parts.dto.ExternalPartSearchResponseDTO;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;

@Service
public class UmapiExternalPartSearchService implements ExternalPartSearchService {

    private static final int DEFAULT_LIMIT = 10;
    private static final int DEFAULT_OFFSET = 0;

    private final UmapiClient umapiClient;
    private final UmapiProperties properties;
    private final ExternalApiRetryExecutor umapiRetryExecutor;
    private final JsonRedisCacheService cacheService;
    private final UmapiArticleNormalizer normalizer;
    private final UmapiCacheKeyFactory cacheKeyFactory;
    private final UmapiCatalogItemMapper mapper;

    public UmapiExternalPartSearchService(UmapiClient umapiClient,
                                          UmapiProperties properties,
                                          @Qualifier("umapiRetryExecutor") ExternalApiRetryExecutor umapiRetryExecutor,
                                          JsonRedisCacheService cacheService,
                                          UmapiArticleNormalizer normalizer,
                                          UmapiCacheKeyFactory cacheKeyFactory,
                                          UmapiCatalogItemMapper mapper) {
        this.umapiClient = umapiClient;
        this.properties = properties;
        this.umapiRetryExecutor = umapiRetryExecutor;
        this.cacheService = cacheService;
        this.normalizer = normalizer;
        this.cacheKeyFactory = cacheKeyFactory;
        this.mapper = mapper;
    }

    @Override
    public ExternalPartSearchResponseDTO search(String articleNumber, String brand, Integer limit, Integer offset) {
        String normalizedArticle = normalizer.normalizeRequiredArticle(articleNumber);
        String normalizedBrand = normalizer.normalizeOptional(brand);
        int normalizedLimit = normalizeLimit(limit);
        int normalizedOffset = normalizeOffset(offset);
        String cacheKey = cacheKeyFactory.searchKey(
                properties.languageCode(),
                properties.regionCode(),
                normalizedArticle,
                normalizedBrand,
                normalizedLimit,
                normalizedOffset
        );

        return cacheService.get(cacheKey, ExternalPartSearchResponseDTO.class)
                .map(this::markCached)
                .orElseGet(() -> fetchAndCache(cacheKey, normalizedArticle, normalizedBrand, normalizedLimit, normalizedOffset));
    }

    private ExternalPartSearchResponseDTO fetchAndCache(String cacheKey,
                                                        String articleNumber,
                                                        String brand,
                                                        int limit,
                                                        int offset) {
        try {
            UmapiAnalogsResponse response = umapiRetryExecutor.execute(
                    () -> umapiClient.findAnalogs(articleNumber, brand, limit, offset)
            );
            Instant cachedAt = Instant.now();
            ExternalPartSearchResponseDTO dto = ExternalPartSearchResponseDTO.builder()
                    .articleNumber(articleNumber)
                    .brand(brand)
                    .cached(false)
                    .fallback(false)
                    .cachedAt(cachedAt)
                    .cacheExpiresAt(cachedAt.plus(properties.cache().searchTtl()))
                    .items(response == null || response.getData() == null
                            ? List.of()
                            : response.getData().stream().map(mapper::map).toList())
                    .build();
            cacheService.put(cacheKey, dto, properties.cache().searchTtl());
            return dto;
        } catch (ExternalApiUnavailableException exception) {
            return cacheService.get(cacheKey, ExternalPartSearchResponseDTO.class)
                    .map(this::markFallback)
                    .orElseThrow(() -> exception);
        }
    }

    private ExternalPartSearchResponseDTO markCached(ExternalPartSearchResponseDTO dto) {
        dto.setCached(true);
        dto.setFallback(false);
        return dto;
    }

    private ExternalPartSearchResponseDTO markFallback(ExternalPartSearchResponseDTO dto) {
        dto.setCached(true);
        dto.setFallback(true);
        return dto;
    }

    private int normalizeLimit(Integer limit) {
        if (limit == null) {
            return DEFAULT_LIMIT;
        }
        if (limit < 1 || limit > 100) {
            throw new IllegalArgumentException("Limit must be between 1 and 100");
        }
        return limit;
    }

    private int normalizeOffset(Integer offset) {
        if (offset == null) {
            return DEFAULT_OFFSET;
        }
        if (offset < 0) {
            throw new IllegalArgumentException("Offset must not be negative");
        }
        return offset;
    }
}
