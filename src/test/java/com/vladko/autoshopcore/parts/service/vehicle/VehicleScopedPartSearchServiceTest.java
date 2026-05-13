package com.vladko.autoshopcore.parts.service.vehicle;

import com.vladko.autoshopcore.integration.umapi.support.UmapiArticleNormalizer;
import com.vladko.autoshopcore.order.entity.Order;
import com.vladko.autoshopcore.order.exception.OrderConflictException;
import com.vladko.autoshopcore.order.repository.OrderRepository;
import com.vladko.autoshopcore.parts.dto.catalog.CatalogArticleResponseDTO;
import com.vladko.autoshopcore.parts.dto.catalog.CatalogArticleSearchResponseDTO;
import com.vladko.autoshopcore.parts.dto.catalog.CatalogProductGroupResponseDTO;
import com.vladko.autoshopcore.parts.dto.catalog.CatalogProductGroupSearchResponseDTO;
import com.vladko.autoshopcore.parts.entity.Part;
import com.vladko.autoshopcore.parts.repository.PartRepository;
import com.vladko.autoshopcore.parts.service.catalog.PartCatalogSearchService;
import com.vladko.autoshopcore.vehicle.entity.Vehicle;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class VehicleScopedPartSearchServiceTest {

    @Mock private OrderRepository orderRepository;
    @Mock private PartCatalogSearchService partCatalogSearchService;
    @Mock private PartRepository partRepository;

    private VehicleScopedPartSearchService service;

    @BeforeEach
    void setUp() {
        service = new VehicleScopedPartSearchServiceImpl(orderRepository, partCatalogSearchService, partRepository, new UmapiArticleNormalizer());
    }

    @Test
    void shouldSearchByNameWithinLinkedVehicleAndEnrichLocalAvailability() {
        Vehicle vehicle = Vehicle.builder()
                .id(10)
                .brand("BMW")
                .model("X5")
                .umapiType("PC")
                .umapiModificationId(333)
                .umapiModificationName("X5 3.0D")
                .build();
        Order order = new Order();
        order.setId(3);
        order.setVehicle(vehicle);

        when(orderRepository.findWithVehicleById(3)).thenReturn(Optional.of(order));
        when(partCatalogSearchService.searchProductGroups("PC", 333, "Oil Filter")).thenReturn(
                CatalogProductGroupSearchResponseDTO.builder()
                        .cached(false)
                        .fallback(false)
                        .items(List.of(CatalogProductGroupResponseDTO.builder()
                                .productGroupId(7)
                                .name("Oil Filter")
                                .normalizedName("oil filter")
                                .build()))
                        .build()
        );
        when(partCatalogSearchService.searchArticles("PC", 333, List.of(7), null, 20, 0)).thenReturn(
                CatalogArticleSearchResponseDTO.builder()
                        .cached(false)
                        .fallback(false)
                        .items(List.of(CatalogArticleResponseDTO.builder()
                                .source("CATALOG")
                                .umapiArticleId(55)
                                .articleNumber("OC90")
                                .brand("MAHLE")
                                .name("Oil Filter")
                                .shortDescription("Oil Filter")
                                .supplierQuoteSearchUrl("/api/procurement/supplier-quotes/search?query=OC90")
                                .build()))
                        .build()
        );
        when(partRepository.findAll()).thenReturn(List.of(Part.builder()
                .id(44)
                .articleNumber("OC90")
                .brand("MAHLE")
                .name("Oil Filter")
                .stockQuantity(5)
                .reservedQuantity(2)
                .build()));

        var response = service.searchByName(3, "Oil Filter", false, null, null);

        assertThat(response.getVehicleId()).isEqualTo(10);
        assertThat(response.getMatchedProductGroups()).hasSize(1);
        assertThat(response.getItems()).hasSize(1);
        assertThat(response.getItems().get(0).getArticleNumber()).isEqualTo("OC90");
        assertThat(response.getItems().get(0).isAvailableLocally()).isTrue();
        assertThat(response.getItems().get(0).isCanAddAsLocal()).isTrue();
        assertThat(response.getItems().get(0).isCanAddAsRequested()).isFalse();
        assertThat(response.getItems().get(0).getMatchedLocalPart()).isNotNull();
        assertThat(response.getItems().get(0).getMatchedLocalPart().getAvailableQuantity()).isEqualTo(3);
    }

    @Test
    void shouldRejectSearchWhenVehicleIsNotLinkedToCatalog() {
        Vehicle vehicle = Vehicle.builder().id(10).brand("BMW").model("X5").build();
        Order order = new Order();
        order.setId(3);
        order.setVehicle(vehicle);
        when(orderRepository.findWithVehicleById(3)).thenReturn(Optional.of(order));

        assertThatThrownBy(() -> service.searchByName(3, "Oil Filter", false, null, null))
                .isInstanceOf(OrderConflictException.class)
                .hasMessageContaining("not linked");
    }
}
