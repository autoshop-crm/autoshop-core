package com.vladko.autoshopcore.parts.service.catalog;

import com.vladko.autoshopcore.integration.shared.ExternalApiRetryExecutor;
import com.vladko.autoshopcore.integration.shared.ExternalApiUnavailableException;
import com.vladko.autoshopcore.integration.shared.JsonRedisCacheService;
import com.vladko.autoshopcore.integration.umapi.client.UmapiClient;
import com.vladko.autoshopcore.integration.umapi.config.UmapiProperties;
import com.vladko.autoshopcore.integration.umapi.mapper.UmapiVehicleCatalogMapper;
import com.vladko.autoshopcore.integration.umapi.support.UmapiCatalogCacheKeyFactory;
import com.vladko.autoshopcore.parts.dto.catalog.CatalogManufacturerResponseDTO;
import com.vladko.autoshopcore.parts.dto.catalog.CatalogModelSeriesResponseDTO;
import com.vladko.autoshopcore.parts.dto.catalog.CatalogModificationResponseDTO;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.Duration;
import java.util.List;
import java.util.Locale;

@Service
public class UmapiVehicleCatalogLookupService implements VehicleCatalogLookupService {

    private static final Duration VEHICLE_TREE_TTL = Duration.ofDays(7);

    private final UmapiClient umapiClient;
    private final UmapiProperties properties;
    private final ExternalApiRetryExecutor retryExecutor;
    private final JsonRedisCacheService cacheService;
    private final UmapiCatalogCacheKeyFactory cacheKeyFactory;
    private final UmapiVehicleCatalogMapper mapper;

    public UmapiVehicleCatalogLookupService(UmapiClient umapiClient,
                                            UmapiProperties properties,
                                            @Qualifier("umapiRetryExecutor") ExternalApiRetryExecutor retryExecutor,
                                            JsonRedisCacheService cacheService,
                                            UmapiCatalogCacheKeyFactory cacheKeyFactory,
                                            UmapiVehicleCatalogMapper mapper) {
        this.umapiClient = umapiClient;
        this.properties = properties;
        this.retryExecutor = retryExecutor;
        this.cacheService = cacheService;
        this.cacheKeyFactory = cacheKeyFactory;
        this.mapper = mapper;
    }

    @Override
    public List<CatalogManufacturerResponseDTO> getManufacturers(String type, Boolean popular) {
        String normalizedType = normalizeType(type);
        boolean popularOnly = popular == null || popular;
        String cacheKey = cacheKeyFactory.manufacturersKey(
                properties.languageCode(),
                properties.regionCode(),
                normalizedType,
                popularOnly
        );

        return cacheService.getList(cacheKey, CatalogManufacturerResponseDTO.class)
                .orElseGet(() -> fetchManufacturers(cacheKey, normalizedType, popularOnly));
    }

    @Override
    public List<CatalogModelSeriesResponseDTO> getModelSeries(String type, Integer manufacturerId) {
        String normalizedType = normalizeType(type);
        requireId(manufacturerId, "Manufacturer id is required");
        String cacheKey = cacheKeyFactory.modelSeriesKey(
                properties.languageCode(),
                properties.regionCode(),
                normalizedType,
                manufacturerId
        );

        return cacheService.getList(cacheKey, CatalogModelSeriesResponseDTO.class)
                .orElseGet(() -> fetchModelSeries(cacheKey, normalizedType, manufacturerId));
    }

    @Override
    public List<CatalogModificationResponseDTO> getModifications(String type, Integer modelSeriesId) {
        String normalizedType = normalizeType(type);
        requireId(modelSeriesId, "Model series id is required");
        String cacheKey = cacheKeyFactory.modificationsKey(
                properties.languageCode(),
                properties.regionCode(),
                normalizedType,
                modelSeriesId
        );

        return cacheService.getList(cacheKey, CatalogModificationResponseDTO.class)
                .orElseGet(() -> fetchModifications(cacheKey, normalizedType, modelSeriesId));
    }

    private List<CatalogManufacturerResponseDTO> fetchManufacturers(String cacheKey, String type, boolean popular) {
        try {
            List<CatalogManufacturerResponseDTO> response = retryExecutor.execute(
                    () -> nullSafe(umapiClient.getManufacturers(type, popular)).stream()
                            .map(item -> mapper.mapManufacturer(type, item))
                            .toList()
            );
            cacheService.put(cacheKey, response, VEHICLE_TREE_TTL);
            return response;
        } catch (ExternalApiUnavailableException exception) {
            return cacheService.getList(cacheKey, CatalogManufacturerResponseDTO.class)
                    .orElseThrow(() -> exception);
        }
    }

    private List<CatalogModelSeriesResponseDTO> fetchModelSeries(String cacheKey,
                                                                 String type,
                                                                 Integer manufacturerId) {
        try {
            List<CatalogModelSeriesResponseDTO> response = retryExecutor.execute(
                    () -> nullSafe(umapiClient.getModelSeries(type, manufacturerId)).stream()
                            .map(item -> mapper.mapModelSeries(type, item))
                            .toList()
            );
            cacheService.put(cacheKey, response, VEHICLE_TREE_TTL);
            return response;
        } catch (ExternalApiUnavailableException exception) {
            return cacheService.getList(cacheKey, CatalogModelSeriesResponseDTO.class)
                    .orElseThrow(() -> exception);
        }
    }

    private List<CatalogModificationResponseDTO> fetchModifications(String cacheKey,
                                                                    String type,
                                                                    Integer modelSeriesId) {
        try {
            List<CatalogModificationResponseDTO> response = retryExecutor.execute(
                    () -> nullSafe(umapiClient.getPassengerModifications(type, modelSeriesId)).stream()
                            .map(item -> mapper.mapModification(type, modelSeriesId, item))
                            .toList()
            );
            cacheService.put(cacheKey, response, VEHICLE_TREE_TTL);
            return response;
        } catch (ExternalApiUnavailableException exception) {
            return cacheService.getList(cacheKey, CatalogModificationResponseDTO.class)
                    .orElseThrow(() -> exception);
        }
    }

    private <T> List<T> nullSafe(List<T> items) {
        return items == null ? List.of() : items;
    }

    private void requireId(Integer value, String message) {
        if (value == null) {
            throw new IllegalArgumentException(message);
        }
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
