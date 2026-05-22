package com.vladko.autoshopcore.vehicle.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.vladko.autoshopcore.shared.exception.GlobalExceptionHandler;
import com.vladko.autoshopcore.vehicle.dto.VehicleCreateDTO;
import com.vladko.autoshopcore.vehicle.dto.VehicleResponseDTO;
import com.vladko.autoshopcore.vehicle.exception.VehicleConflictException;
import com.vladko.autoshopcore.vehicle.exception.VehicleNotFoundException;
import com.vladko.autoshopcore.vehicle.service.VehicleService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(VehicleController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(GlobalExceptionHandler.class)
class VehicleControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private VehicleService vehicleService;

    @Test
    void createShouldReturnCreatedVehicle() throws Exception {
        VehicleCreateDTO dto = VehicleCreateDTO.builder()
                .customerId(1)
                .brand("Toyota")
                .model("Camry")
                .vin("JT2BG22KXV0123456")
                .licensePlate("A123BC77")
                .build();

        when(vehicleService.create(any(VehicleCreateDTO.class))).thenReturn(VehicleResponseDTO.builder()
                .id(15)
                .customerId(1)
                .brand("Toyota")
                .model("Camry")
                .vin("JT2BG22KXV0123456")
                .licensePlate("A123BC77")
                .createdAt(Instant.parse("2026-04-13T10:15:30Z"))
                .updatedAt(Instant.parse("2026-04-13T10:15:30Z"))
                .build());

        mockMvc.perform(post("/api/vehicles")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(15))
                .andExpect(jsonPath("$.customerId").value(1))
                .andExpect(jsonPath("$.vin").value("JT2BG22KXV0123456"));
    }

    @Test
    void createShouldReturnBadRequestForInvalidPayload() throws Exception {
        VehicleCreateDTO dto = VehicleCreateDTO.builder()
                .customerId(null)
                .brand("")
                .model("")
                .vin("123")
                .licensePlate("!")
                .build();

        mockMvc.perform(post("/api/vehicles")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));
    }

    @Test
    void getByIdShouldReturnNotFound() throws Exception {
        when(vehicleService.getById(404)).thenThrow(new VehicleNotFoundException(404));

        mockMvc.perform(get("/api/vehicles/404"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Vehicle with id '404' was not found"));
    }

    @Test
    void createShouldReturnConflictWhenVinAlreadyExists() throws Exception {
        VehicleCreateDTO dto = VehicleCreateDTO.builder()
                .customerId(1)
                .brand("Toyota")
                .model("Camry")
                .vin("JT2BG22KXV0123456")
                .licensePlate("A123BC77")
                .build();

        when(vehicleService.create(any(VehicleCreateDTO.class)))
                .thenThrow(new VehicleConflictException("Vehicle with vin 'JT2BG22KXV0123456' already exists"));

        mockMvc.perform(post("/api/vehicles")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("Vehicle with vin 'JT2BG22KXV0123456' already exists"));
    }

    @Test
    void linkCatalogShouldReturnLinkedVehicle() throws Exception {
        when(vehicleService.linkCatalog(any(), any())).thenReturn(VehicleResponseDTO.builder()
                .id(15)
                .customerId(1)
                .brand("Toyota")
                .model("Camry")
                .vin("JT2BG22KXV0123456")
                .licensePlate("A123BC77")
                .umapiType("PC")
                .umapiModificationId(333)
                .umapiModificationName("Camry 2.5")
                .umapiCatalogLinkedAt(Instant.parse("2026-04-22T10:00:00Z"))
                .build());

        mockMvc.perform(put("/api/vehicles/15/catalog-link")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "type": "PC",
                                  "manufacturerId": 111,
                                  "manufacturerName": "TOYOTA",
                                  "modelSeriesId": 222,
                                  "modelSeriesName": "CAMRY",
                                  "modificationId": 333,
                                  "modificationName": "Camry 2.5",
                                  "engineDescription": "2.5, petrol, 181 hp"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.umapiType").value("PC"))
                .andExpect(jsonPath("$.umapiModificationId").value(333));
    }

    @Test
    void unlinkCatalogShouldReturnNoContent() throws Exception {
        mockMvc.perform(delete("/api/vehicles/15/catalog-link"))
                .andExpect(status().isNoContent());
    }
}
