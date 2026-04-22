package com.vladko.autoshopcore.parts.controller;

import com.vladko.autoshopcore.parts.dto.catalog.CatalogArticleSearchResponseDTO;
import com.vladko.autoshopcore.parts.dto.catalog.CatalogManufacturerResponseDTO;
import com.vladko.autoshopcore.parts.dto.catalog.CatalogModelSeriesResponseDTO;
import com.vladko.autoshopcore.parts.dto.catalog.CatalogModificationResponseDTO;
import com.vladko.autoshopcore.parts.dto.catalog.CatalogProductGroupSearchResponseDTO;
import com.vladko.autoshopcore.parts.service.catalog.PartCatalogSearchService;
import com.vladko.autoshopcore.parts.service.catalog.VehicleCatalogLookupService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/parts/catalog")
@RequiredArgsConstructor
public class CatalogPartSearchController {

    private final VehicleCatalogLookupService vehicleCatalogLookupService;
    private final PartCatalogSearchService partCatalogSearchService;

    @GetMapping("/manufacturers")
    public ResponseEntity<List<CatalogManufacturerResponseDTO>> getManufacturers(
            @RequestParam(defaultValue = "PC") String type,
            @RequestParam(required = false) Boolean popular) {
        return ResponseEntity.ok(vehicleCatalogLookupService.getManufacturers(type, popular));
    }

    @GetMapping("/model-series")
    public ResponseEntity<List<CatalogModelSeriesResponseDTO>> getModelSeries(
            @RequestParam(defaultValue = "PC") String type,
            @RequestParam Integer manufacturerId) {
        return ResponseEntity.ok(vehicleCatalogLookupService.getModelSeries(type, manufacturerId));
    }

    @GetMapping("/modifications")
    public ResponseEntity<List<CatalogModificationResponseDTO>> getModifications(
            @RequestParam(defaultValue = "PC") String type,
            @RequestParam Integer modelSeriesId) {
        return ResponseEntity.ok(vehicleCatalogLookupService.getModifications(type, modelSeriesId));
    }

    @GetMapping("/product-groups/search")
    public ResponseEntity<CatalogProductGroupSearchResponseDTO> searchProductGroups(
            @RequestParam(defaultValue = "PC") String type,
            @RequestParam Integer modificationId,
            @RequestParam String query) {
        return ResponseEntity.ok(partCatalogSearchService.searchProductGroups(type, modificationId, query));
    }

    @GetMapping("/articles")
    public ResponseEntity<CatalogArticleSearchResponseDTO> searchArticles(
            @RequestParam(defaultValue = "PC") String type,
            @RequestParam Integer modificationId,
            @RequestParam List<Integer> productGroupIds,
            @RequestParam(required = false) Integer supplierId,
            @RequestParam(required = false) Integer limit,
            @RequestParam(required = false) Integer offset) {
        return ResponseEntity.ok(partCatalogSearchService.searchArticles(
                type,
                modificationId,
                productGroupIds,
                supplierId,
                limit,
                offset
        ));
    }
}
