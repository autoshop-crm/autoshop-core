package com.vladko.autoshopcore.servicecatalog.service;

import com.vladko.autoshopcore.servicecatalog.dto.ServiceCatalogItemResponseDTO;
import com.vladko.autoshopcore.servicecatalog.dto.ServiceCatalogItemUpsertDTO;
import com.vladko.autoshopcore.servicecatalog.dto.ServiceCategoryResponseDTO;
import com.vladko.autoshopcore.servicecatalog.dto.ServiceCategoryUpsertDTO;

import java.util.List;

public interface ServiceCatalogService {
    ServiceCategoryResponseDTO createCategory(ServiceCategoryUpsertDTO dto);
    List<ServiceCategoryResponseDTO> getCategories(boolean activeOnly);
    ServiceCatalogItemResponseDTO createService(ServiceCatalogItemUpsertDTO dto);
    ServiceCatalogItemResponseDTO updateService(Integer id, ServiceCatalogItemUpsertDTO dto);
    List<ServiceCatalogItemResponseDTO> getServices(boolean activeOnly, Integer categoryId);
}
