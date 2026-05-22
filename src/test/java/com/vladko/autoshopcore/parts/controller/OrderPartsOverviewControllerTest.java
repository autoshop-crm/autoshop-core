package com.vladko.autoshopcore.parts.controller;

import com.vladko.autoshopcore.parts.dto.OrderPartOverviewItemDTO;
import com.vladko.autoshopcore.parts.dto.OrderPartsOverviewResponseDTO;
import com.vladko.autoshopcore.parts.service.OrderPartsOverviewService;
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

@WebMvcTest(OrderPartsOverviewController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(GlobalExceptionHandler.class)
class OrderPartsOverviewControllerTest {
    @Autowired MockMvc mockMvc;
    @MockitoBean OrderPartsOverviewService overviewService;

    @Test
    void overviewShouldReturnItems() throws Exception {
        when(overviewService.getOverview(3)).thenReturn(OrderPartsOverviewResponseDTO.builder().orderId(3).items(List.of(OrderPartOverviewItemDTO.builder().itemType("REQUESTED").articleNumber("OF123").build())).build());
        mockMvc.perform(get("/api/orders/3/parts/overview"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[0].itemType").value("REQUESTED"));
    }
}
