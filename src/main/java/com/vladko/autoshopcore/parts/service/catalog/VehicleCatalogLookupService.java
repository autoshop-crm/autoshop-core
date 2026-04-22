package com.vladko.autoshopcore.parts.service.catalog;

import com.vladko.autoshopcore.parts.dto.catalog.CatalogManufacturerResponseDTO;
import com.vladko.autoshopcore.parts.dto.catalog.CatalogModelSeriesResponseDTO;
import com.vladko.autoshopcore.parts.dto.catalog.CatalogModificationResponseDTO;

import java.util.List;

public interface VehicleCatalogLookupService {

    List<CatalogManufacturerResponseDTO> getManufacturers(String type, Boolean popular);

    List<CatalogModelSeriesResponseDTO> getModelSeries(String type, Integer manufacturerId);

    List<CatalogModificationResponseDTO> getModifications(String type, Integer modelSeriesId);
}
