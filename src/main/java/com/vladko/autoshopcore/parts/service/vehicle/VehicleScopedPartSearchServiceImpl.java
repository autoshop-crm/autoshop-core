package com.vladko.autoshopcore.parts.service.vehicle;

import com.vladko.autoshopcore.integration.umapi.support.UmapiArticleNormalizer;
import com.vladko.autoshopcore.order.entity.Order;
import com.vladko.autoshopcore.order.exception.OrderConflictException;
import com.vladko.autoshopcore.order.exception.OrderNotFoundException;
import com.vladko.autoshopcore.order.repository.OrderRepository;
import com.vladko.autoshopcore.parts.dto.PartResponseDTO;
import com.vladko.autoshopcore.parts.dto.catalog.CatalogArticleResponseDTO;
import com.vladko.autoshopcore.parts.dto.catalog.CatalogArticleSearchResponseDTO;
import com.vladko.autoshopcore.parts.dto.catalog.CatalogProductGroupResponseDTO;
import com.vladko.autoshopcore.parts.dto.catalog.CatalogProductGroupSearchResponseDTO;
import com.vladko.autoshopcore.parts.dto.vehicle.VehicleScopedPartSearchItemDTO;
import com.vladko.autoshopcore.parts.dto.vehicle.VehicleScopedPartSearchResponseDTO;
import com.vladko.autoshopcore.parts.entity.Part;
import com.vladko.autoshopcore.parts.repository.PartRepository;
import com.vladko.autoshopcore.parts.service.catalog.PartCatalogSearchService;
import com.vladko.autoshopcore.vehicle.entity.Vehicle;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

@Service
@RequiredArgsConstructor
public class VehicleScopedPartSearchServiceImpl implements VehicleScopedPartSearchService {

    private static final int DEFAULT_LIMIT = 20;
    private static final int DEFAULT_OFFSET = 0;
    private static final int MAX_PRODUCT_GROUPS = 5;

    private final OrderRepository orderRepository;
    private final PartCatalogSearchService partCatalogSearchService;
    private final PartRepository partRepository;
    private final UmapiArticleNormalizer articleNormalizer;

    @Override
    @Transactional(readOnly = true)
    public VehicleScopedPartSearchResponseDTO searchByName(Integer orderId,
                                                           String query,
                                                           Boolean availableOnly,
                                                           Integer limit,
                                                           Integer offset) {
        Order order = orderRepository.findWithVehicleById(orderId)
                .orElseThrow(() -> new OrderNotFoundException(orderId));
        Vehicle vehicle = requireLinkedVehicle(order);
        String normalizedQuery = normalizeQuery(query);
        int normalizedLimit = normalizeLimit(limit);
        int normalizedOffset = normalizeOffset(offset);
        boolean filterAvailableOnly = Boolean.TRUE.equals(availableOnly);

        CatalogProductGroupSearchResponseDTO productGroups = partCatalogSearchService.searchProductGroups(
                vehicle.getUmapiType(),
                vehicle.getUmapiModificationId(),
                normalizedQuery
        );
        List<CatalogProductGroupResponseDTO> matchedGroups = productGroups.getItems().stream()
                .limit(MAX_PRODUCT_GROUPS)
                .toList();

        if (matchedGroups.isEmpty()) {
            return VehicleScopedPartSearchResponseDTO.builder()
                    .orderId(orderId)
                    .vehicleId(vehicle.getId())
                    .vehicleBrand(vehicle.getBrand())
                    .vehicleModel(vehicle.getModel())
                    .modificationId(vehicle.getUmapiModificationId())
                    .modificationName(vehicle.getUmapiModificationName())
                    .query(normalizedQuery)
                    .catalogLinked(true)
                    .productGroupsCached(productGroups.isCached())
                    .productGroupsFallback(productGroups.isFallback())
                    .articlesCached(false)
                    .articlesFallback(false)
                    .matchedProductGroups(matchedGroups)
                    .items(List.of())
                    .build();
        }

        CatalogArticleSearchResponseDTO articles = partCatalogSearchService.searchArticles(
                vehicle.getUmapiType(),
                vehicle.getUmapiModificationId(),
                matchedGroups.stream().map(CatalogProductGroupResponseDTO::getProductGroupId).toList(),
                null,
                normalizedLimit,
                normalizedOffset
        );

        Map<String, Part> localByArticle = indexLocalParts();
        Map<Integer, CatalogProductGroupResponseDTO> groupsById = matchedGroups.stream()
                .filter(group -> group.getProductGroupId() != null)
                .collect(LinkedHashMap::new, (map, group) -> map.put(group.getProductGroupId(), group), Map::putAll);

        List<VehicleScopedPartSearchItemDTO> items = articles.getItems().stream()
                .map(article -> mapArticle(article, groupsById, localByArticle))
                .filter(item -> !filterAvailableOnly || item.isAvailableLocally())
                .toList();

        return VehicleScopedPartSearchResponseDTO.builder()
                .orderId(orderId)
                .vehicleId(vehicle.getId())
                .vehicleBrand(vehicle.getBrand())
                .vehicleModel(vehicle.getModel())
                .modificationId(vehicle.getUmapiModificationId())
                .modificationName(vehicle.getUmapiModificationName())
                .query(normalizedQuery)
                .catalogLinked(true)
                .productGroupsCached(productGroups.isCached())
                .productGroupsFallback(productGroups.isFallback())
                .articlesCached(articles.isCached())
                .articlesFallback(articles.isFallback())
                .matchedProductGroups(matchedGroups)
                .items(items)
                .build();
    }

    private Vehicle requireLinkedVehicle(Order order) {
        Vehicle vehicle = order.getVehicle();
        if (vehicle == null) {
            throw new OrderConflictException("Order does not have linked vehicle");
        }
        if (vehicle.getUmapiModificationId() == null || !StringUtils.hasText(vehicle.getUmapiType())) {
            throw new OrderConflictException("Vehicle is not linked to UMAPI catalog modification");
        }
        return vehicle;
    }

    private String normalizeQuery(String query) {
        if (query == null || query.trim().isEmpty()) {
            throw new IllegalArgumentException("Query must not be blank");
        }
        return query.trim();
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

    private Map<String, Part> indexLocalParts() {
        Map<String, Part> indexed = new LinkedHashMap<>();
        for (Part part : partRepository.findAll()) {
            if (part.getArticleNumber() == null) {
                continue;
            }
            indexed.putIfAbsent(normalizeArticle(part.getArticleNumber()), part);
        }
        return indexed;
    }

    private VehicleScopedPartSearchItemDTO mapArticle(CatalogArticleResponseDTO article,
                                                      Map<Integer, CatalogProductGroupResponseDTO> groupsById,
                                                      Map<String, Part> localByArticle) {
        Part matchedPart = localByArticle.get(normalizeArticle(article.getArticleNumber()));
        PartResponseDTO matchedLocalPart = matchedPart == null ? null : mapPart(matchedPart);
        boolean availableLocally = matchedLocalPart != null && matchedLocalPart.getAvailableQuantity() != null
                && matchedLocalPart.getAvailableQuantity() > 0;
        CatalogProductGroupResponseDTO group = resolveGroup(article, groupsById);

        return VehicleScopedPartSearchItemDTO.builder()
                .productGroupId(group == null ? null : group.getProductGroupId())
                .productGroupName(group == null ? null : group.getName())
                .umapiArticleId(article.getUmapiArticleId())
                .articleNumber(article.getArticleNumber())
                .brand(article.getBrand())
                .name(article.getName())
                .shortDescription(article.getShortDescription())
                .source(article.getSource())
                .mediaFile(article.getMediaFile())
                .supplierQuoteSearchUrl(article.getSupplierQuoteSearchUrl())
                .matchedLocalPart(matchedLocalPart)
                .exactLocalMatch(matchedLocalPart != null)
                .availableLocally(availableLocally)
                .canAddAsLocal(availableLocally)
                .canAddAsRequested(!availableLocally)
                .build();
    }

    private CatalogProductGroupResponseDTO resolveGroup(CatalogArticleResponseDTO article,
                                                        Map<Integer, CatalogProductGroupResponseDTO> groupsById) {
        if (article.getShortDescription() == null) {
            return groupsById.values().stream().findFirst().orElse(null);
        }
        String normalizedDescription = article.getShortDescription().toLowerCase(Locale.ROOT);
        return groupsById.values().stream()
                .filter(group -> group.getName() != null)
                .filter(group -> normalizedDescription.contains(group.getName().toLowerCase(Locale.ROOT))
                        || (group.getNormalizedName() != null
                        && normalizedDescription.contains(group.getNormalizedName().toLowerCase(Locale.ROOT))))
                .findFirst()
                .orElseGet(() -> groupsById.values().stream().findFirst().orElse(null));
    }

    private String normalizeArticle(String articleNumber) {
        return articleNormalizer.normalizeRequiredArticle(articleNumber);
    }

    private PartResponseDTO mapPart(Part part) {
        int availableQuantity = Math.max(0, safe(part.getStockQuantity()) - safe(part.getReservedQuantity()));
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

    private int safe(Integer value) {
        return Objects.requireNonNullElse(value, 0);
    }
}
