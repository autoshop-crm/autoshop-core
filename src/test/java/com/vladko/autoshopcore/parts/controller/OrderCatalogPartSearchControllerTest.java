package com.vladko.autoshopcore.parts.controller;

import com.vladko.autoshopcore.client.entity.Customer;
import com.vladko.autoshopcore.order.entity.Order;
import com.vladko.autoshopcore.order.repository.OrderRepository;
import com.vladko.autoshopcore.parts.dto.catalog.CatalogProductGroupSearchResponseDTO;
import com.vladko.autoshopcore.parts.service.catalog.PartCatalogSearchService;
import com.vladko.autoshopcore.shared.exception.GlobalExceptionHandler;
import com.vladko.autoshopcore.vehicle.entity.Vehicle;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Optional;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(OrderCatalogPartSearchController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(GlobalExceptionHandler.class)
class OrderCatalogPartSearchControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private OrderRepository orderRepository;

    @MockitoBean
    private PartCatalogSearchService partCatalogSearchService;

    @Test
    void searchProductGroupsShouldUseVehicleCatalogLinkFromOrder() throws Exception {
        Vehicle vehicle = linkedVehicle();
        when(orderRepository.findWithVehicleById(10)).thenReturn(Optional.of(Order.builder()
                .id(10)
                .vehicle(vehicle)
                .customer(Customer.builder().id(1).build())
                .problem("Oil")
                .build()));
        when(partCatalogSearchService.searchProductGroups("PC", 333, "масляный фильтр"))
                .thenReturn(CatalogProductGroupSearchResponseDTO.builder()
                        .type("PC")
                        .modificationId(333)
                        .query("масляный фильтр")
                        .build());

        mockMvc.perform(get("/api/orders/10/parts/catalog/product-groups/search")
                        .param("query", "масляный фильтр"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.modificationId").value(333));
    }

    @Test
    void searchProductGroupsShouldReturnConflictWhenVehicleNotLinked() throws Exception {
        when(orderRepository.findWithVehicleById(10)).thenReturn(Optional.of(Order.builder()
                .id(10)
                .vehicle(Vehicle.builder().id(5).build())
                .customer(Customer.builder().id(1).build())
                .problem("Oil")
                .build()));

        mockMvc.perform(get("/api/orders/10/parts/catalog/product-groups/search")
                        .param("query", "масляный фильтр"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("Vehicle is not linked to UMAPI catalog modification"));
    }

    private Vehicle linkedVehicle() {
        return Vehicle.builder()
                .id(5)
                .umapiType("PC")
                .umapiModificationId(333)
                .build();
    }
}
