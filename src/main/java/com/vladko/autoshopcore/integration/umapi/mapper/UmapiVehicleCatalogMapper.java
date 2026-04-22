package com.vladko.autoshopcore.integration.umapi.mapper;

import com.vladko.autoshopcore.integration.umapi.dto.catalog.UmapiManufacturerResponse;
import com.vladko.autoshopcore.integration.umapi.dto.catalog.UmapiModelSeriesResponse;
import com.vladko.autoshopcore.integration.umapi.dto.catalog.UmapiPassengerModificationResponse;
import com.vladko.autoshopcore.parts.dto.catalog.CatalogManufacturerResponseDTO;
import com.vladko.autoshopcore.parts.dto.catalog.CatalogModelSeriesResponseDTO;
import com.vladko.autoshopcore.parts.dto.catalog.CatalogModificationResponseDTO;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;

@Component
public class UmapiVehicleCatalogMapper {

    public CatalogManufacturerResponseDTO mapManufacturer(String type, UmapiManufacturerResponse item) {
        return CatalogManufacturerResponseDTO.builder()
                .type(type)
                .manufacturerId(item.getMfaId())
                .name(item.getManufacturer())
                .build();
    }

    public CatalogModelSeriesResponseDTO mapModelSeries(String type, UmapiModelSeriesResponse item) {
        return CatalogModelSeriesResponseDTO.builder()
                .type(type)
                .manufacturerId(item.getManufacturerId())
                .modelSeriesId(item.getModelSeriesId())
                .name(item.getModelSeries())
                .productionFrom(item.getProductionFrom())
                .productionTo(item.getProductionTo())
                .build();
    }

    public CatalogModificationResponseDTO mapModification(String type,
                                                          Integer modelSeriesId,
                                                          UmapiPassengerModificationResponse item) {
        return CatalogModificationResponseDTO.builder()
                .type(type)
                .modelSeriesId(modelSeriesId)
                .modificationId(item.getModificationId())
                .name(item.getName())
                .powerPs(item.getPowerPs())
                .capacityLiters(item.getCapacityLiters())
                .engineType(item.getEngineType())
                .bodyType(item.getBodyType())
                .fuelType(item.getFuelType())
                .displayName(displayName(item))
                .build();
    }

    private String displayName(UmapiPassengerModificationResponse item) {
        List<String> parts = new ArrayList<>();
        addIfPresent(parts, item.getName());
        if (item.getPowerPs() != null) {
            parts.add("%s hp".formatted(item.getPowerPs()));
        }
        addIfPresent(parts, item.getFuelType());
        addIfPresent(parts, item.getBodyType());
        return String.join(", ", parts);
    }

    private void addIfPresent(List<String> parts, String value) {
        if (StringUtils.hasText(value)) {
            parts.add(value);
        }
    }
}
