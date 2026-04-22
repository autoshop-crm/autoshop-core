package com.vladko.autoshopcore.integration.umapi.support;

import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.Locale;

@Component
public class UmapiArticleNormalizer {

    public String normalizeRequiredArticle(String value) {
        String normalizedValue = normalizeOptional(value);
        if (!StringUtils.hasText(normalizedValue)) {
            throw new IllegalArgumentException("Article number must not be blank");
        }
        return normalizedValue;
    }

    public String normalizeOptional(String value) {
        if (value == null) {
            return null;
        }
        String normalizedValue = value.trim();
        return normalizedValue.isEmpty() ? null : normalizedValue.toUpperCase(Locale.ROOT);
    }
}
