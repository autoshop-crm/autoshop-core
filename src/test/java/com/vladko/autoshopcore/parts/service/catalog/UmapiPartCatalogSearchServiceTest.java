package com.vladko.autoshopcore.parts.service.catalog;

import com.vladko.autoshopcore.integration.shared.ExternalApiRetryExecutor;
import com.vladko.autoshopcore.integration.shared.JsonRedisCacheService;
import com.vladko.autoshopcore.integration.umapi.client.UmapiClient;
import com.vladko.autoshopcore.integration.umapi.config.UmapiProperties;
import com.vladko.autoshopcore.integration.umapi.dto.UmapiArticleItem;
import com.vladko.autoshopcore.integration.umapi.dto.catalog.UmapiFuseProductGroupResponse;
import com.vladko.autoshopcore.integration.umapi.mapper.UmapiCatalogArticleMapper;
import com.vladko.autoshopcore.integration.umapi.mapper.UmapiProductGroupMapper;
import com.vladko.autoshopcore.integration.umapi.support.CatalogProductGroupMatcher;
import com.vladko.autoshopcore.integration.umapi.support.CatalogSearchTextNormalizer;
import com.vladko.autoshopcore.integration.umapi.support.UmapiCatalogCacheKeyFactory;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class UmapiPartCatalogSearchServiceTest {

    private final UmapiClient umapiClient = mock(UmapiClient.class);
    private final JsonRedisCacheService cacheService = mock(JsonRedisCacheService.class);
    private final CatalogSearchTextNormalizer normalizer = new CatalogSearchTextNormalizer();
    private final UmapiPartCatalogSearchService service = new UmapiPartCatalogSearchService(
            umapiClient,
            properties(),
            new ExternalApiRetryExecutor(1, Duration.ZERO),
            cacheService,
            normalizer,
            new CatalogProductGroupMatcher(normalizer),
            new UmapiCatalogCacheKeyFactory(),
            new UmapiProductGroupMapper(),
            new UmapiCatalogArticleMapper()
    );

    @Test
    void searchProductGroupsShouldNormalizeQueryScoreAndCacheResponse() {
        when(cacheService.get(anyString(), eq(com.vladko.autoshopcore.parts.dto.catalog.CatalogProductGroupSearchResponseDTO.class)))
                .thenReturn(Optional.empty());
        when(cacheService.getList(anyString(), eq(UmapiFuseProductGroupResponse.class)))
                .thenReturn(Optional.empty());
        when(umapiClient.getFuseProductGroups("PC", 333)).thenReturn(List.of(
                group(7, "Масляный фильтр"),
                group(8, "Свеча зажигания")
        ));

        var response = service.searchProductGroups("pc", 333, "масло фильтр");

        assertThat(response.isCached()).isFalse();
        assertThat(response.getQuery()).isEqualTo("масляный фильтр");
        assertThat(response.getItems()).hasSize(1);
        assertThat(response.getItems().get(0).getProductGroupId()).isEqualTo(7);
        assertThat(response.getItems().get(0).getScore()).isEqualByComparingTo("1.00");
        verify(cacheService, atLeastOnce()).put(anyString(), any(), any(Duration.class));
    }

    @Test
    void searchArticlesShouldSortProductGroupIdsAndMapArticle() {
        when(cacheService.get(anyString(), eq(com.vladko.autoshopcore.parts.dto.catalog.CatalogArticleSearchResponseDTO.class)))
                .thenReturn(Optional.empty());
        UmapiArticleItem item = new UmapiArticleItem();
        item.setArticleId(987);
        item.setArticleNumber("90915YZZE1");
        item.setBrand("TOYOTA");
        item.setCompleteDescription("Oil filter");
        when(umapiClient.getArticles("PC", List.of(7, 8), 333, null, 10, 0)).thenReturn(List.of(item));

        var response = service.searchArticles("PC", 333, List.of(8, 7), null, null, null);

        assertThat(response.getProductGroupIds()).containsExactly(7, 8);
        assertThat(response.getItems()).hasSize(1);
        assertThat(response.getItems().get(0).getArticleNumber()).isEqualTo("90915YZZE1");
    }

    private UmapiFuseProductGroupResponse group(Integer id, String description) {
        UmapiFuseProductGroupResponse group = new UmapiFuseProductGroupResponse();
        group.setProductGroupId(id);
        group.setDescription(description);
        return group;
    }

    private UmapiProperties properties() {
        return new UmapiProperties(
                "https://api.umapi.ru",
                "umapi-key",
                "ru",
                "WWW",
                Duration.ofSeconds(1),
                Duration.ofSeconds(5),
                new UmapiProperties.Cache(Duration.ofHours(6)),
                new UmapiProperties.Retry(1, Duration.ZERO)
        );
    }
}
