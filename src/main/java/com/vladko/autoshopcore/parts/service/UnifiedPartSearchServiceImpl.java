package com.vladko.autoshopcore.parts.service;

import com.vladko.autoshopcore.integration.umapi.support.UmapiArticleNormalizer;
import com.vladko.autoshopcore.parts.dto.ExternalPartCatalogItemResponseDTO;
import com.vladko.autoshopcore.parts.dto.ExternalPartSearchResponseDTO;
import com.vladko.autoshopcore.parts.dto.PartResponseDTO;
import com.vladko.autoshopcore.parts.dto.UnifiedPartSearchItemResponseDTO;
import com.vladko.autoshopcore.parts.dto.UnifiedPartSearchResponseDTO;
import com.vladko.autoshopcore.parts.entity.Part;
import com.vladko.autoshopcore.parts.repository.PartRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class UnifiedPartSearchServiceImpl implements UnifiedPartSearchService {

    private final PartService partService;
    private final ExternalPartSearchService externalPartSearchService;
    private final PartRepository partRepository;
    private final UmapiArticleNormalizer umapiArticleNormalizer;

    @Override
    @Transactional(readOnly = true)
    public UnifiedPartSearchResponseDTO search(String articleNumber,
                                               String brand,
                                               Boolean availableOnly,
                                               Integer limit,
                                               Integer offset) {
        List<PartResponseDTO> localParts = partService.search(articleNumber, brand, null, availableOnly);
        ExternalPartSearchResponseDTO external = externalPartSearchService.search(articleNumber, brand, limit, offset);

        Map<String, PartResponseDTO> localByArticle = new LinkedHashMap<>();
        for (PartResponseDTO localPart : localParts) {
            localByArticle.put(normalizeArticle(localPart.getArticleNumber()), localPart);
        }

        Set<String> externalArticles = external.getItems().stream()
                .map(ExternalPartCatalogItemResponseDTO::getArticleNumber)
                .filter(Objects::nonNull)
                .map(this::normalizeArticleSafe)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

        List<String> missingArticles = externalArticles.stream()
                .filter(article -> !localByArticle.containsKey(article))
                .toList();

        if (!missingArticles.isEmpty()) {
            List<Part> matchedParts = partRepository.findAllByArticleNumberIn(missingArticles);
            matchedParts.stream()
                    .sorted(Comparator.comparing(Part::getId))
                    .forEach(part -> localByArticle.putIfAbsent(normalizeArticle(part.getArticleNumber()), mapLocal(part)));
        }

        List<UnifiedPartSearchItemResponseDTO> items = new ArrayList<>();
        for (PartResponseDTO localPart : localParts) {
            items.add(UnifiedPartSearchItemResponseDTO.builder()
                    .sourceType("LOCAL")
                    .articleNumber(localPart.getArticleNumber())
                    .brand(localPart.getBrand())
                    .name(localPart.getName())
                    .localPart(localPart)
                    .matchedLocalPart(localPart)
                    .exactLocalMatch(true)
                    .availableLocally(localPart.getAvailableQuantity() != null && localPart.getAvailableQuantity() > 0)
                    .build());
        }

        for (ExternalPartCatalogItemResponseDTO externalItem : external.getItems()) {
            PartResponseDTO matchedLocalPart = localByArticle.get(normalizeArticleSafe(externalItem.getArticleNumber()));
            items.add(UnifiedPartSearchItemResponseDTO.builder()
                    .sourceType("EXTERNAL")
                    .articleNumber(externalItem.getArticleNumber())
                    .brand(externalItem.getBrand())
                    .name(externalItem.getName())
                    .externalPart(externalItem)
                    .matchedLocalPart(matchedLocalPart)
                    .exactLocalMatch(matchedLocalPart != null)
                    .availableLocally(matchedLocalPart != null
                            && matchedLocalPart.getAvailableQuantity() != null
                            && matchedLocalPart.getAvailableQuantity() > 0)
                    .build());
        }

        return UnifiedPartSearchResponseDTO.builder()
                .articleNumber(external.getArticleNumber())
                .brand(external.getBrand())
                .externalCached(external.isCached())
                .externalFallback(external.isFallback())
                .items(items)
                .build();
    }

    private String normalizeArticle(String articleNumber) {
        return umapiArticleNormalizer.normalizeRequiredArticle(articleNumber);
    }

    private String normalizeArticleSafe(String articleNumber) {
        if (articleNumber == null || articleNumber.trim().isEmpty()) {
            return null;
        }
        return normalizeArticle(articleNumber);
    }

    private PartResponseDTO mapLocal(Part part) {
        Integer availableQuantity = part.getStockQuantity() - part.getReservedQuantity();
        return PartResponseDTO.builder()
                .id(part.getId())
                .brand(part.getBrand())
                .name(part.getName())
                .articleNumber(part.getArticleNumber())
                .cost(part.getCost())
                .stockQuantity(part.getStockQuantity())
                .reservedQuantity(part.getReservedQuantity())
                .availableQuantity(availableQuantity)
                .createdAt(part.getCreatedAt())
                .updatedAt(part.getUpdatedAt())
                .build();
    }
}
