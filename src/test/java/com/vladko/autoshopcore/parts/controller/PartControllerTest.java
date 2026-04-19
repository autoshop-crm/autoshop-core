package com.vladko.autoshopcore.parts.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.vladko.autoshopcore.parts.dto.PartCreateDTO;
import com.vladko.autoshopcore.parts.dto.PartResponseDTO;
import com.vladko.autoshopcore.parts.dto.PartStockUpdateDTO;
import com.vladko.autoshopcore.parts.exception.PartConflictException;
import com.vladko.autoshopcore.parts.exception.PartNotFoundException;
import com.vladko.autoshopcore.parts.service.PartService;
import com.vladko.autoshopcore.shared.exception.GlobalExceptionHandler;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(PartController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(GlobalExceptionHandler.class)
class PartControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private PartService partService;

    @Test
    void createShouldReturnCreatedPart() throws Exception {
        when(partService.create(any(PartCreateDTO.class))).thenReturn(PartResponseDTO.builder()
                .id(10)
                .brand("Bosch")
                .name("Oil filter")
                .articleNumber("OF-123")
                .cost(new BigDecimal("15.50"))
                .stockQuantity(0)
                .reservedQuantity(0)
                .availableQuantity(0)
                .createdAt(Instant.parse("2026-04-16T10:15:30Z"))
                .updatedAt(Instant.parse("2026-04-16T10:15:30Z"))
                .build());

        mockMvc.perform(post("/api/parts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(PartCreateDTO.builder()
                                .brand("Bosch")
                                .name("Oil filter")
                                .articleNumber("OF-123")
                                .cost(new BigDecimal("15.50"))
                                .build())))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(10))
                .andExpect(jsonPath("$.articleNumber").value("OF-123"));
    }

    @Test
    void createShouldReturnBadRequestForInvalidPayload() throws Exception {
        mockMvc.perform(post("/api/parts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(PartCreateDTO.builder()
                                .brand("Bosch")
                                .name("")
                                .articleNumber("")
                                .cost(new BigDecimal("-1.00"))
                                .build())))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));
    }

    @Test
    void getByIdShouldReturnNotFound() throws Exception {
        when(partService.getById(404)).thenThrow(new PartNotFoundException(404));

        mockMvc.perform(get("/api/parts/404"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Part with id '404' was not found"));
    }

    @Test
    void updateStockShouldReturnBadRequestWhenStockLowerThanReserved() throws Exception {
        when(partService.updateStock(eq(10), any(PartStockUpdateDTO.class)))
                .thenThrow(new IllegalArgumentException("Stock quantity cannot be lower than reserved quantity"));

        mockMvc.perform(put("/api/parts/10/stock")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new PartStockUpdateDTO(1))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Stock quantity cannot be lower than reserved quantity"));
    }

    @Test
    void searchShouldReturnFilteredParts() throws Exception {
        when(partService.search("OF-123", null, null, true)).thenReturn(List.of(
                PartResponseDTO.builder()
                        .id(10)
                        .brand("Bosch")
                        .name("Oil filter")
                        .articleNumber("OF-123")
                        .cost(new BigDecimal("15.50"))
                        .stockQuantity(5)
                        .reservedQuantity(1)
                        .availableQuantity(4)
                        .build()
        ));

        mockMvc.perform(get("/api/parts")
                        .param("articleNumber", "OF-123")
                        .param("availableOnly", "true"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].availableQuantity").value(4));
    }

    @Test
    void deleteShouldReturnConflictWhenPartUsedInOrders() throws Exception {
        doThrow(new PartConflictException("Part with id '10' is already used in orders"))
                .when(partService).delete(10);

        mockMvc.perform(delete("/api/parts/10"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("Part with id '10' is already used in orders"));
    }
}
