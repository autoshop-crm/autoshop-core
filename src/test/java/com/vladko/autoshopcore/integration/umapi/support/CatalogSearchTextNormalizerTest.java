package com.vladko.autoshopcore.integration.umapi.support;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class CatalogSearchTextNormalizerTest {

    private final CatalogSearchTextNormalizer normalizer = new CatalogSearchTextNormalizer();

    @Test
    void normalizeShouldTrimLowercaseReplaceYoAndSquashWhitespace() {
        assertThat(normalizer.normalize("  МАСЛЯННЫЙ,   ФИЛЬТР Ё  "))
                .isEqualTo("маслянный фильтр е");
    }

    @Test
    void normalizeShouldApplySynonyms() {
        assertThat(normalizer.normalize("Воздухан")).isEqualTo("воздушный фильтр");
        assertThat(normalizer.normalize("стойка стаба")).isEqualTo("стойка стабилизатора");
        assertThat(normalizer.normalize("лямбда-зонд")).isEqualTo("датчик кислорода");
    }
}
