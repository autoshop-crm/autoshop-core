package com.vladko.autoshopcore.integration.umapi.support;

import com.vladko.autoshopcore.integration.umapi.dto.catalog.UmapiFuseProductGroupResponse;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

class CatalogProductGroupMatcherTest {

    private final CatalogProductGroupMatcher matcher = new CatalogProductGroupMatcher(
            new CatalogSearchTextNormalizer()
    );

    @Test
    void scoreShouldReturnExactMatchScore() {
        assertThat(matcher.score("масляный фильтр", group("Масляный фильтр")))
                .contains(new BigDecimal("1.00"));
    }

    @Test
    void scoreShouldRankContainsHigherThanTokenPartial() {
        assertThat(matcher.score("фильтр", group("Масляный фильтр")).orElseThrow())
                .isEqualByComparingTo("0.90");
        assertThat(matcher.score("фильтр топливный", group("Масляный фильтр")).orElseThrow())
                .isEqualByComparingTo("0.55");
    }

    @Test
    void scoreShouldIgnoreUnrelatedGroup() {
        assertThat(matcher.score("датчик кислорода", group("Свеча зажигания"))).isEmpty();
    }

    private UmapiFuseProductGroupResponse group(String description) {
        UmapiFuseProductGroupResponse group = new UmapiFuseProductGroupResponse();
        group.setProductGroupId(7);
        group.setDescription(description);
        return group;
    }
}
