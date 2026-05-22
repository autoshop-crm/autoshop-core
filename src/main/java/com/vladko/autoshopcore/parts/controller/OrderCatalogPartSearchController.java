package com.vladko.autoshopcore.parts.controller;

import com.vladko.autoshopcore.order.entity.Order;
import com.vladko.autoshopcore.order.exception.OrderConflictException;
import com.vladko.autoshopcore.order.exception.OrderNotFoundException;
import com.vladko.autoshopcore.order.repository.OrderRepository;
import com.vladko.autoshopcore.parts.dto.catalog.CatalogArticleSearchResponseDTO;
import com.vladko.autoshopcore.parts.dto.catalog.CatalogProductGroupSearchResponseDTO;
import com.vladko.autoshopcore.parts.service.catalog.PartCatalogSearchService;
import com.vladko.autoshopcore.vehicle.entity.Vehicle;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/orders/{orderId}/parts/catalog")
@RequiredArgsConstructor
public class OrderCatalogPartSearchController {

    private final OrderRepository orderRepository;
    private final PartCatalogSearchService partCatalogSearchService;

    @GetMapping("/product-groups/search")
    public ResponseEntity<CatalogProductGroupSearchResponseDTO> searchProductGroups(
            @PathVariable Integer orderId,
            @RequestParam String query) {
        Vehicle vehicle = findLinkedVehicle(orderId);
        return ResponseEntity.ok(partCatalogSearchService.searchProductGroups(
                vehicle.getUmapiType(),
                vehicle.getUmapiModificationId(),
                query
        ));
    }

    @GetMapping("/articles")
    public ResponseEntity<CatalogArticleSearchResponseDTO> searchArticles(
            @PathVariable Integer orderId,
            @RequestParam List<Integer> productGroupIds,
            @RequestParam(required = false) Integer supplierId,
            @RequestParam(required = false) Integer limit,
            @RequestParam(required = false) Integer offset) {
        Vehicle vehicle = findLinkedVehicle(orderId);
        return ResponseEntity.ok(partCatalogSearchService.searchArticles(
                vehicle.getUmapiType(),
                vehicle.getUmapiModificationId(),
                productGroupIds,
                supplierId,
                limit,
                offset
        ));
    }

    private Vehicle findLinkedVehicle(Integer orderId) {
        Order order = orderRepository.findWithVehicleById(orderId)
                .orElseThrow(() -> new OrderNotFoundException(orderId));
        Vehicle vehicle = order.getVehicle();
        if (vehicle.getUmapiModificationId() == null || !StringUtils.hasText(vehicle.getUmapiType())) {
            throw new OrderConflictException("Vehicle is not linked to UMAPI catalog modification");
        }
        return vehicle;
    }
}
