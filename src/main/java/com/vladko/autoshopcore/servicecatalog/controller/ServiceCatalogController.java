package com.vladko.autoshopcore.servicecatalog.controller;

import com.vladko.autoshopcore.servicecatalog.dto.ServiceCatalogItemResponseDTO;
import com.vladko.autoshopcore.servicecatalog.dto.ServiceCatalogItemUpsertDTO;
import com.vladko.autoshopcore.servicecatalog.dto.ServiceCategoryResponseDTO;
import com.vladko.autoshopcore.servicecatalog.dto.ServiceCategoryUpsertDTO;
import com.vladko.autoshopcore.servicecatalog.service.ServiceCatalogService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/service-catalog")
@RequiredArgsConstructor
public class ServiceCatalogController {

    private final ServiceCatalogService serviceCatalogService;

    @PostMapping("/categories")
    public ResponseEntity<ServiceCategoryResponseDTO> createCategory(@Valid @RequestBody ServiceCategoryUpsertDTO dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(serviceCatalogService.createCategory(dto));
    }

    @GetMapping("/categories")
    public ResponseEntity<List<ServiceCategoryResponseDTO>> getCategories(@RequestParam(defaultValue = "true") boolean activeOnly) {
        return ResponseEntity.ok(serviceCatalogService.getCategories(activeOnly));
    }

    @PostMapping("/services")
    public ResponseEntity<ServiceCatalogItemResponseDTO> createService(@Valid @RequestBody ServiceCatalogItemUpsertDTO dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(serviceCatalogService.createService(dto));
    }

    @PutMapping("/services/{id}")
    public ResponseEntity<ServiceCatalogItemResponseDTO> updateService(@PathVariable Integer id,
                                                                       @Valid @RequestBody ServiceCatalogItemUpsertDTO dto) {
        return ResponseEntity.ok(serviceCatalogService.updateService(id, dto));
    }

    @GetMapping("/services")
    public ResponseEntity<List<ServiceCatalogItemResponseDTO>> getServices(@RequestParam(defaultValue = "true") boolean activeOnly,
                                                                           @RequestParam(required = false) Integer categoryId) {
        return ResponseEntity.ok(serviceCatalogService.getServices(activeOnly, categoryId));
    }
}
