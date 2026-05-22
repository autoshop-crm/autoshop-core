package com.vladko.autoshopcore.integration.umapi.support;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class UmapiCatalogCacheKeyFactoryTest {

    private final UmapiCatalogCacheKeyFactory factory = new UmapiCatalogCacheKeyFactory();

    @Test
    void articlesKeyShouldIncludeSortedProductGroupsFromCallerAndPaging() {
        assertThat(factory.articlesKey("ru", "WWW", "PC", 333, List.of(7, 8), null, 10, 0))
                .isEqualTo("umapi:catalog:articles:ru:WWW:PC:333:7,8:ANY:10:0");
    }

    @Test
    void productGroupSearchKeyShouldHashQuery() {
        String key = factory.productGroupSearchKey("ru", "WWW", "PC", 333, "масляный фильтр");

        assertThat(key).startsWith("umapi:catalog:product-groups-search:ru:WWW:PC:333:");
        assertThat(key).doesNotContain("масляный");
    }
}
