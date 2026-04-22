package com.vladko.autoshopcore.parts.service.catalog;

import com.vladko.autoshopcore.integration.shared.ExternalApiRetryExecutor;
import com.vladko.autoshopcore.integration.shared.ExternalApiUnavailableException;
import com.vladko.autoshopcore.integration.shared.JsonRedisCacheService;
import com.vladko.autoshopcore.integration.umapi.client.UmapiClient;
import com.vladko.autoshopcore.integration.umapi.config.UmapiProperties;
import com.vladko.autoshopcore.integration.umapi.dto.catalog.UmapiFuseProductGroupResponse;
import com.vladko.autoshopcore.integration.umapi.mapper.UmapiCatalogArticleMapper;
import com.vladko.autoshopcore.integration.umapi.mapper.UmapiProductGroupMapper;
import com.vladko.autoshopcore.integration.umapi.support.CatalogProductGroupMatcher;
import com.vladko.autoshopcore.integration.umapi.support.CatalogSearchTextNormalizer;
import com.vladko.autoshopcore.integration.umapi.support.UmapiCatalogCacheKeyFactory;
import com.vladko.autoshopcore.parts.dto.catalog.CatalogArticleSearchResponseDTO;
import com.vladko.autoshopcore.parts.dto.catalog.CatalogProductGroupSearchResponseDTO;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

@Service
public class UmapiPartCatalogSearchService implements PartCatalogSearchService {

    private static final BigDecimal MIN_SCORE = new BigDecimal("0.35");
    private static final int MAX_PRODUCT_GROUP_RESULTS = 20;
    private static final int DEFAULT_LIMIT = 10;
    private static final int DEFAULT_OFFSET = 0;
    private static final Duration PRODUCT_GROUP_TTL = Duration.ofHours(24);
    private static final Duration ARTICLE_TTL = Duration.ofHours(6);
    private static final Duration EMPTY_RESULT_TTL = Duration.ofMinutes(15);

    private final UmapiClient umapiClient;
    private final UmapiProperties properties;
    private final ExternalApiRetryExecutor retryExecutor;
    private final JsonRedisCacheService cacheService;
    private final CatalogSearchTextNormalizer textNormalizer;
    private final CatalogProductGroupMatcher matcher;
    private final UmapiCatalogCacheKeyFactory cacheKeyFactory;
    private final UmapiProductGroupMapper productGroupMapper;
    private final UmapiCatalogArticleMapper articleMapper;

    public UmapiPartCatalogSearchService(UmapiClient umapiClient,
                                         UmapiProperties properties,
                                         @Qualifier("umapiRetryExecutor") ExternalApiRetryExecutor retryExecutor,
                                         JsonRedisCacheService cacheService,
                                         CatalogSearchTextNormalizer textNormalizer,
                                         CatalogProductGroupMatcher matcher,
                                         UmapiCatalogCacheKeyFactory cacheKeyFactory,
                                         UmapiProductGroupMapper productGroupMapper,
                                         UmapiCatalogArticleMapper articleMapper) {
        this.umapiClient = umapiClient;
        this.properties = properties;
        this.retryExecutor = retryExecutor;
        this.cacheService = cacheService;
        this.textNormalizer = textNormalizer;
        this.matcher = matcher;
        this.cacheKeyFactory = cacheKeyFactory;
        this.productGroupMapper = productGroupMapper;
        this.articleMapper = articleMapper;
    }

    @Override
    public CatalogProductGroupSearchResponseDTO searchProductGroups(String type,
                                                                    Integer modificationId,
                                                                    String query) {
        String normalizedType = normalizeType(type);
        requireModificationId(modificationId);
        String normalizedQuery = textNormalizer.normalizeRequired(
                query,
                "Product group query must not be blank"
        );
        String searchCacheKey = cacheKeyFactory.productGroupSearchKey(
                properties.languageCode(),
                properties.regionCode(),
                normalizedType,
                modificationId,
                normalizedQuery
        );

        return cacheService.get(searchCacheKey, CatalogProductGroupSearchResponseDTO.class)
                .map(this::markCached)
                .orElseGet(() -> fetchProductGroups(searchCacheKey, normalizedType, modificationId, normalizedQuery));
    }

    @Override
    public CatalogArticleSearchResponseDTO searchArticles(String type,
                                                          Integer modificationId,
                                                          List<Integer> productGroupIds,
                                                          Integer supplierId,
                                                          Integer limit,
                                                          Integer offset) {
        String normalizedType = normalizeType(type);
        requireModificationId(modificationId);
        List<Integer> normalizedProductGroupIds = normalizeProductGroupIds(productGroupIds);
        int normalizedLimit = normalizeLimit(limit);
        int normalizedOffset = normalizeOffset(offset);
        String cacheKey = cacheKeyFactory.articlesKey(
                properties.languageCode(),
                properties.regionCode(),
                normalizedType,
                modificationId,
                normalizedProductGroupIds,
                supplierId,
                normalizedLimit,
                normalizedOffset
        );

        return cacheService.get(cacheKey, CatalogArticleSearchResponseDTO.class)
                .map(this::markCached)
                .orElseGet(() -> fetchArticles(
                        cacheKey,
                        normalizedType,
                        modificationId,
                        normalizedProductGroupIds,
                        supplierId,
                        normalizedLimit,
                        normalizedOffset
                ));
    }

    private CatalogProductGroupSearchResponseDTO fetchProductGroups(String searchCacheKey,
                                                                    String type,
                                                                    Integer modificationId,
                                                                    String normalizedQuery) {
        try {
            List<UmapiFuseProductGroupResponse> fuseProductGroups = getFuseProductGroups(type, modificationId);
            Instant cachedAt = Instant.now();
            var items = fuseProductGroups.stream()
                    .map(group -> matcher.score(normalizedQuery, group)
                            .filter(score -> score.compareTo(MIN_SCORE) >= 0)
                            .map(score -> productGroupMapper.map(group, score))
                            .orElse(null))
                    .filter(Objects::nonNull)
                    .sorted(Comparator.comparing(
                            item -> item.getScore(),
                            Comparator.reverseOrder()
                    ))
                    .limit(MAX_PRODUCT_GROUP_RESULTS)
                    .toList();
            Duration ttl = ttlForSize(items.size(), PRODUCT_GROUP_TTL);
            CatalogProductGroupSearchResponseDTO response = CatalogProductGroupSearchResponseDTO.builder()
                    .type(type)
                    .modificationId(modificationId)
                    .query(normalizedQuery)
                    .cached(false)
                    .fallback(false)
                    .cachedAt(cachedAt)
                    .cacheExpiresAt(cachedAt.plus(ttl))
                    .items(items)
                    .build();
            cacheService.put(searchCacheKey, response, ttl);
            return response;
        } catch (ExternalApiUnavailableException exception) {
            return cacheService.get(searchCacheKey, CatalogProductGroupSearchResponseDTO.class)
                    .map(this::markFallback)
                    .orElseThrow(() -> exception);
        }
    }

    private CatalogArticleSearchResponseDTO fetchArticles(String cacheKey,
                                                         String type,
                                                         Integer modificationId,
                                                         List<Integer> productGroupIds,
                                                         Integer supplierId,
                                                         int limit,
                                                         int offset) {
        try {
            Instant cachedAt = Instant.now();
            var articles = retryExecutor.execute(() -> umapiClient.getArticles(
                    type,
                    productGroupIds,
                    modificationId,
                    supplierId,
                    limit,
                    offset
            ));
            CatalogArticleSearchResponseDTO response = CatalogArticleSearchResponseDTO.builder()
                    .type(type)
                    .modificationId(modificationId)
                    .productGroupIds(productGroupIds)
                    .supplierId(supplierId)
                    .limit(limit)
                    .offset(offset)
                    .cached(false)
                    .fallback(false)
                    .cachedAt(cachedAt)
                    .cacheExpiresAt(cachedAt.plus(ttlForSize(nullSafe(articles).size(), ARTICLE_TTL)))
                    .items(nullSafe(articles).stream()
                            .map(articleMapper::map)
                            .toList())
                    .build();
            cacheService.put(cacheKey, response, ttlForSize(response.getItems().size(), ARTICLE_TTL));
            return response;
        } catch (ExternalApiUnavailableException exception) {
            return cacheService.get(cacheKey, CatalogArticleSearchResponseDTO.class)
                    .map(this::markFallback)
                    .orElseThrow(() -> exception);
        }
    }

    private List<UmapiFuseProductGroupResponse> getFuseProductGroups(String type, Integer modificationId) {
        String fuseCacheKey = cacheKeyFactory.fuseKey(
                properties.languageCode(),
                properties.regionCode(),
                type,
                modificationId
        );

        return cacheService.getList(fuseCacheKey, UmapiFuseProductGroupResponse.class)
                .orElseGet(() -> {
                    List<UmapiFuseProductGroupResponse> response = nullSafe(retryExecutor.execute(
                            () -> umapiClient.getFuseProductGroups(type, modificationId)
                    ));
                    cacheService.put(fuseCacheKey, response, ttlForSize(response.size(), PRODUCT_GROUP_TTL));
                    return response;
                });
    }

    private CatalogProductGroupSearchResponseDTO markCached(CatalogProductGroupSearchResponseDTO dto) {
        dto.setCached(true);
        dto.setFallback(false);
        return dto;
    }

    private CatalogProductGroupSearchResponseDTO markFallback(CatalogProductGroupSearchResponseDTO dto) {
        dto.setCached(true);
        dto.setFallback(true);
        return dto;
    }

    private CatalogArticleSearchResponseDTO markCached(CatalogArticleSearchResponseDTO dto) {
        dto.setCached(true);
        dto.setFallback(false);
        return dto;
    }

    private CatalogArticleSearchResponseDTO markFallback(CatalogArticleSearchResponseDTO dto) {
        dto.setCached(true);
        dto.setFallback(true);
        return dto;
    }

    private <T> List<T> nullSafe(List<T> items) {
        return items == null ? List.of() : items;
    }

    private Duration ttlForSize(int size, Duration defaultTtl) {
        return size == 0 ? EMPTY_RESULT_TTL : defaultTtl;
    }

    private void requireModificationId(Integer modificationId) {
        if (modificationId == null) {
            throw new IllegalArgumentException("Modification id is required");
        }
    }

    private List<Integer> normalizeProductGroupIds(List<Integer> productGroupIds) {
        if (productGroupIds == null || productGroupIds.isEmpty()) {
            throw new IllegalArgumentException("Product group ids are required");
        }
        List<Integer> normalizedIds = productGroupIds.stream()
                .filter(Objects::nonNull)
                .distinct()
                .sorted()
                .toList();
        if (normalizedIds.isEmpty()) {
            throw new IllegalArgumentException("Product group ids are required");
        }
        return normalizedIds;
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

    private String normalizeType(String type) {
        if (!StringUtils.hasText(type)) {
            throw new IllegalArgumentException("Catalog vehicle type is not supported");
        }
        String normalizedType = type.trim().toUpperCase(Locale.ROOT);
        if (!"PC".equals(normalizedType)) {
            throw new IllegalArgumentException("Catalog vehicle type is not supported");
        }
        return normalizedType;
    }
}
