package com.vladko.autoshopcore.integration.umapi.support;

import com.vladko.autoshopcore.integration.umapi.dto.catalog.UmapiFuseProductGroupResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Arrays;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class CatalogProductGroupMatcher {

    private static final BigDecimal EXACT_MATCH_SCORE = new BigDecimal("1.00");
    private static final BigDecimal GROUP_CONTAINS_QUERY_SCORE = new BigDecimal("0.90");
    private static final BigDecimal QUERY_CONTAINS_GROUP_SCORE = new BigDecimal("0.85");
    private static final BigDecimal ALL_TOKENS_SCORE = new BigDecimal("0.75");
    private static final BigDecimal PARTIAL_TOKEN_BASE_SCORE = new BigDecimal("0.40");
    private static final BigDecimal PARTIAL_TOKEN_BONUS_SCORE = new BigDecimal("0.30");

    private final CatalogSearchTextNormalizer normalizer;

    public Optional<BigDecimal> score(String normalizedQuery, UmapiFuseProductGroupResponse group) {
        String normalizedGroupName = normalizer.normalize(firstText(
                group.getNormalizedDescription(),
                group.getDescription()
        ));
        if (!StringUtils.hasText(normalizedQuery) || !StringUtils.hasText(normalizedGroupName)) {
            return Optional.empty();
        }
        if (normalizedQuery.equals(normalizedGroupName)) {
            return Optional.of(EXACT_MATCH_SCORE);
        }
        if (normalizedGroupName.contains(normalizedQuery)) {
            return Optional.of(GROUP_CONTAINS_QUERY_SCORE);
        }
        if (normalizedQuery.contains(normalizedGroupName)) {
            return Optional.of(QUERY_CONTAINS_GROUP_SCORE);
        }

        Set<String> queryTokens = tokens(normalizedQuery);
        Set<String> groupTokens = tokens(normalizedGroupName);
        long matchedTokens = queryTokens.stream()
                .filter(groupTokens::contains)
                .count();
        if (matchedTokens == queryTokens.size() && !queryTokens.isEmpty()) {
            return Optional.of(ALL_TOKENS_SCORE);
        }
        if (matchedTokens > 0) {
            BigDecimal ratio = BigDecimal.valueOf(matchedTokens)
                    .divide(BigDecimal.valueOf(queryTokens.size()), 2, RoundingMode.HALF_UP);
            return Optional.of(PARTIAL_TOKEN_BASE_SCORE.add(PARTIAL_TOKEN_BONUS_SCORE.multiply(ratio)));
        }
        return Optional.empty();
    }

    private Set<String> tokens(String value) {
        return Arrays.stream(value.split("\\s+"))
                .filter(StringUtils::hasText)
                .collect(Collectors.toSet());
    }

    private String firstText(String primary, String fallback) {
        return StringUtils.hasText(primary) ? primary : fallback;
    }
}
