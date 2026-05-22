package com.vladko.autoshopcore.parts.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.vladko.autoshopcore.parts.dto.vehicle.VehicleScopedPartSearchResponseDTO;
import com.vladko.autoshopcore.parts.service.vehicle.VehicleScopedPartSearchService;
import com.vladko.autoshopcore.shared.exception.GlobalExceptionHandler;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(OrderVehicleScopedPartSearchController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(GlobalExceptionHandler.class)
class OrderVehicleScopedPartSearchControllerTest {

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;

    @MockitoBean VehicleScopedPartSearchService vehicleScopedPartSearchService;

    @Test
    void shouldReturnVehicleScopedSearchResult() throws Exception {
        when(vehicleScopedPartSearchService.searchByName(3, "Oil Filter", false, 10, 0))
                .thenReturn(VehicleScopedPartSearchResponseDTO.builder()
                        .orderId(3)
                        .vehicleId(10)
                        .query("Oil Filter")
                        .catalogLinked(true)
                        .build());

        mockMvc.perform(get("/api/orders/3/parts/search-by-name")
                        .param("query", "Oil Filter")
                        .param("availableOnly", "false")
                        .param("limit", "10")
                        .param("offset", "0"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.orderId").value(3))
                .andExpect(jsonPath("$.vehicleId").value(10))
                .andExpect(jsonPath("$.catalogLinked").value(true));
    }
}
