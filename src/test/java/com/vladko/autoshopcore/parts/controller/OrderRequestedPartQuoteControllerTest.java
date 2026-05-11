package com.vladko.autoshopcore.parts.controller;

import com.vladko.autoshopcore.parts.service.OrderRequestedPartQuoteService;
import com.vladko.autoshopcore.procurement.dto.SupplierQuoteSearchResponseDTO;
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

@WebMvcTest(OrderRequestedPartQuoteController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(GlobalExceptionHandler.class)
class OrderRequestedPartQuoteControllerTest {
    @Autowired MockMvc mockMvc;
    @MockitoBean OrderRequestedPartQuoteService quoteService;

    @Test
    void quotesShouldReturnCarretaResponse() throws Exception {
        when(quoteService.getQuotes(3, 11)).thenReturn(SupplierQuoteSearchResponseDTO.builder().query("OF123").provider("CARRETA").build());
        mockMvc.perform(get("/api/orders/3/requested-parts/11/quotes"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.provider").value("CARRETA"));
    }
}
