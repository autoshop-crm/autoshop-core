package com.vladko.autoshopcore.client.controller;

import com.vladko.autoshopcore.parts.dto.catalog.CatalogManufacturerResponseDTO;
import com.vladko.autoshopcore.parts.dto.catalog.CatalogModelSeriesResponseDTO;
import com.vladko.autoshopcore.parts.dto.catalog.CatalogModificationResponseDTO;
import com.vladko.autoshopcore.parts.service.catalog.VehicleCatalogLookupService;
import com.vladko.autoshopcore.shared.exception.GlobalExceptionHandler;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(CustomerVehicleCatalogLookupController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(GlobalExceptionHandler.class)
class CustomerVehicleCatalogLookupControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private VehicleCatalogLookupService vehicleCatalogLookupService;

    @Test
    void getManufacturersShouldReturnCustomerCatalogManufacturers() throws Exception {
        when(vehicleCatalogLookupService.getManufacturers("PC", true)).thenReturn(List.of(
                CatalogManufacturerResponseDTO.builder()
                        .type("PC")
                        .manufacturerId(111)
                        .name("TOYOTA")
                        .build()
        ));

        mockMvc.perform(get("/api/customers/me/vehicles/catalog/manufacturers")
                        .param("type", "PC")
                        .param("popular", "true"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].manufacturerId").value(111))
                .andExpect(jsonPath("$[0].name").value("TOYOTA"));
    }

    @Test
    void getModelSeriesShouldReturnCustomerCatalogModelSeries() throws Exception {
        when(vehicleCatalogLookupService.getModelSeries("PC", 111)).thenReturn(List.of(
                CatalogModelSeriesResponseDTO.builder()
                        .type("PC")
                        .manufacturerId(111)
                        .modelSeriesId(222)
                        .name("X5")
                        .build()
        ));

        mockMvc.perform(get("/api/customers/me/vehicles/catalog/model-series")
                        .param("type", "PC")
                        .param("manufacturerId", "111"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].modelSeriesId").value(222));
    }

    @Test
    void getModificationsShouldReturnCustomerCatalogModifications() throws Exception {
        when(vehicleCatalogLookupService.getModifications("PC", 222)).thenReturn(List.of(
                CatalogModificationResponseDTO.builder()
                        .type("PC")
                        .modelSeriesId(222)
                        .modificationId(333)
                        .displayName("X5 3.0D")
                        .build()
        ));

        mockMvc.perform(get("/api/customers/me/vehicles/catalog/modifications")
                        .param("type", "PC")
                        .param("modelSeriesId", "222"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].modificationId").value(333));
    }
}
