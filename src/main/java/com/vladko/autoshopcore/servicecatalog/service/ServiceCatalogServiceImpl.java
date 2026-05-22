package com.vladko.autoshopcore.servicecatalog.service;

import com.vladko.autoshopcore.entities.Services;
import com.vladko.autoshopcore.servicecatalog.dto.ServiceCatalogItemResponseDTO;
import com.vladko.autoshopcore.servicecatalog.dto.ServiceCatalogItemUpsertDTO;
import com.vladko.autoshopcore.servicecatalog.dto.ServiceCategoryResponseDTO;
import com.vladko.autoshopcore.servicecatalog.dto.ServiceCategoryUpsertDTO;
import com.vladko.autoshopcore.servicecatalog.entity.ServiceCategory;
import com.vladko.autoshopcore.servicecatalog.entity.ServiceInspectionItem;
import com.vladko.autoshopcore.servicecatalog.repository.ServiceCategoryRepository;
import com.vladko.autoshopcore.servicecatalog.repository.ServiceInspectionItemRepository;
import com.vladko.autoshopcore.servicecatalog.repository.ServicesRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ServiceCatalogServiceImpl implements ServiceCatalogService {

    private final ServicesRepository servicesRepository;
    private final ServiceCategoryRepository serviceCategoryRepository;
    private final ServiceInspectionItemRepository serviceInspectionItemRepository;

    @Override
    @Transactional
    public ServiceCategoryResponseDTO createCategory(ServiceCategoryUpsertDTO dto) {
        ServiceCategory category = ServiceCategory.builder()
                .name(dto.getName().trim())
                .displayOrder(dto.getDisplayOrder())
                .active(dto.getActive() == null ? true : dto.getActive())
                .build();
        return map(serviceCategoryRepository.save(category));
    }

    @Override
    @Transactional(readOnly = true)
    public List<ServiceCategoryResponseDTO> getCategories(boolean activeOnly) {
        List<ServiceCategory> categories = activeOnly
                ? serviceCategoryRepository.findAllByActiveTrueOrderByDisplayOrderAscIdAsc()
                : serviceCategoryRepository.findAll();
        return categories.stream().map(this::map).toList();
    }

    @Override
    @Transactional
    public ServiceCatalogItemResponseDTO createService(ServiceCatalogItemUpsertDTO dto) {
        Services service = new Services();
        apply(service, dto);
        Services saved = servicesRepository.save(service);
        replaceInspectionItems(saved, dto.getInspectionItems());
        return map(saved);
    }

    @Override
    @Transactional
    public ServiceCatalogItemResponseDTO updateService(Integer id, ServiceCatalogItemUpsertDTO dto) {
        Services service = servicesRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Service not found: " + id));
        apply(service, dto);
        Services saved = servicesRepository.save(service);
        replaceInspectionItems(saved, dto.getInspectionItems());
        return map(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ServiceCatalogItemResponseDTO> getServices(boolean activeOnly, Integer categoryId) {
        List<Services> services;
        if (categoryId != null) {
            services = servicesRepository.findAllByCategoryIdOrderByNameAsc(categoryId);
        } else if (activeOnly) {
            services = servicesRepository.findAllByActiveTrueOrderByNameAsc();
        } else {
            services = servicesRepository.findAll();
        }
        return services.stream().map(this::map).toList();
    }

    private void apply(Services service, ServiceCatalogItemUpsertDTO dto) {
        service.setName(dto.getName().trim());
        service.setDescription(blankToNull(dto.getDescription()));
        service.setBasePrice(dto.getBasePrice());
        service.setActive(dto.getActive() == null ? true : dto.getActive());
        service.setDefaultDurationMinutes(dto.getDefaultDurationMinutes());
        if (dto.getCategoryId() == null) {
            service.setCategory(null);
        } else {
            service.setCategory(serviceCategoryRepository.findById(dto.getCategoryId())
                    .orElseThrow(() -> new EntityNotFoundException("Category not found: " + dto.getCategoryId())));
        }
    }

    private void replaceInspectionItems(Services service, List<String> titles) {
        List<ServiceInspectionItem> existing = serviceInspectionItemRepository.findAllByServiceIdOrderByIdAsc(service.getId());
        serviceInspectionItemRepository.deleteAll(existing);
        if (titles == null) {
            return;
        }
        List<ServiceInspectionItem> replacement = titles.stream()
                .map(String::trim)
                .filter(value -> !value.isEmpty())
                .map(value -> ServiceInspectionItem.builder().service(service).title(value).active(true).build())
                .toList();
        serviceInspectionItemRepository.saveAll(replacement);
    }

    private ServiceCatalogItemResponseDTO map(Services service) {
        List<String> inspectionItems = service.getId() == null
                ? Collections.emptyList()
                : serviceInspectionItemRepository.findAllByServiceIdOrderByIdAsc(service.getId()).stream()
                .map(ServiceInspectionItem::getTitle)
                .toList();
        return ServiceCatalogItemResponseDTO.builder()
                .id(service.getId())
                .name(service.getName())
                .description(service.getDescription())
                .basePrice(service.getBasePrice())
                .active(service.getActive())
                .categoryId(service.getCategory() == null ? null : service.getCategory().getId())
                .categoryName(service.getCategory() == null ? null : service.getCategory().getName())
                .defaultDurationMinutes(service.getDefaultDurationMinutes())
                .inspectionItems(inspectionItems)
                .build();
    }

    private ServiceCategoryResponseDTO map(ServiceCategory category) {
        return ServiceCategoryResponseDTO.builder()
                .id(category.getId())
                .name(category.getName())
                .displayOrder(category.getDisplayOrder())
                .active(category.getActive())
                .build();
    }

    private String blankToNull(String value) {
        if (value == null) {
            return null;
        }
        String normalized = value.trim();
        return normalized.isEmpty() ? null : normalized;
    }
}
