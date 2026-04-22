package com.vladko.autoshopcore.integration.umapi.support;

import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

@Component
public class CatalogSearchTextNormalizer {

    private static final Map<String, String> SYNONYMS = new LinkedHashMap<>();

    static {
        SYNONYMS.put("лямбда зонд", "датчик кислорода");
        SYNONYMS.put("масло фильтр", "масляный фильтр");
        SYNONYMS.put("воздухан", "воздушный фильтр");
        SYNONYMS.put("колодки", "тормозные колодки");
        SYNONYMS.put("стойка стаба", "стойка стабилизатора");
    }

    public String normalizeRequired(String value, String message) {
        String normalizedValue = normalize(value);
        if (!StringUtils.hasText(normalizedValue)) {
            throw new IllegalArgumentException(message);
        }
        return normalizedValue;
    }

    public String normalize(String value) {
        if (value == null) {
            return null;
        }

        String normalizedValue = value.trim()
                .toLowerCase(Locale.ROOT)
                .replace('ё', 'е')
                .replaceAll("[\\p{Punct}&&[^-]]", " ")
                .replace('-', ' ')
                .replaceAll("\\s+", " ")
                .trim();

        return SYNONYMS.getOrDefault(normalizedValue, normalizedValue);
    }
}
