package com.vladko.autoshopcore.client.controller;

import com.vladko.autoshopcore.parts.dto.catalog.CatalogManufacturerResponseDTO;
import com.vladko.autoshopcore.parts.dto.catalog.CatalogModelSeriesResponseDTO;
import com.vladko.autoshopcore.parts.dto.catalog.CatalogModificationResponseDTO;
import com.vladko.autoshopcore.parts.service.catalog.VehicleCatalogLookupService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/customers/me/vehicles/catalog")
@RequiredArgsConstructor
public class CustomerVehicleCatalogLookupController {

    private final VehicleCatalogLookupService vehicleCatalogLookupService;

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
}
