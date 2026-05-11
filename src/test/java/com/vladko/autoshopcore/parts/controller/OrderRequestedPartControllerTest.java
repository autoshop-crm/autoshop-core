package com.vladko.autoshopcore.parts.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.vladko.autoshopcore.parts.dto.OrderRequestedPartCreateDTO;
import com.vladko.autoshopcore.parts.dto.OrderRequestedPartResponseDTO;
import com.vladko.autoshopcore.parts.entity.OrderRequestedPartStatus;
import com.vladko.autoshopcore.parts.service.OrderRequestedPartService;
import com.vladko.autoshopcore.shared.exception.GlobalExceptionHandler;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(OrderRequestedPartController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(GlobalExceptionHandler.class)
class OrderRequestedPartControllerTest {

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;

    @MockitoBean OrderRequestedPartService service;

    @Test
    void createShouldReturnCreated() throws Exception {
        when(service.create(any(), any())).thenReturn(OrderRequestedPartResponseDTO.builder().id(11).orderId(3).status(OrderRequestedPartStatus.OUT_OF_STOCK).articleNumber("OF123").build());
        mockMvc.perform(post("/api/orders/3/requested-parts")
                        .contentType(APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(OrderRequestedPartCreateDTO.builder().articleNumber("OF123").name("Oil Filter").quantity(1).build())))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("OUT_OF_STOCK"));
    }

    @Test
    void getAllShouldReturnList() throws Exception {
        when(service.getAllByOrderId(3)).thenReturn(List.of(OrderRequestedPartResponseDTO.builder().id(11).articleNumber("OF123").status(OrderRequestedPartStatus.OUT_OF_STOCK).build()));
        mockMvc.perform(get("/api/orders/3/requested-parts"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].articleNumber").value("OF123"));
    }
}
