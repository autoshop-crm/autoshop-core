package com.vladko.autoshopcore.procurement.controller;

import com.vladko.autoshopcore.procurement.dto.SupplierQuoteResponseDTO;
import com.vladko.autoshopcore.procurement.dto.SupplierQuoteSearchResponseDTO;
import com.vladko.autoshopcore.procurement.service.SupplierQuoteService;
import com.vladko.autoshopcore.shared.exception.GlobalExceptionHandler;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(SupplierQuoteController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(GlobalExceptionHandler.class)
class SupplierQuoteControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private SupplierQuoteService supplierQuoteService;

    @Test
    void searchShouldReturnCarretaQuotes() throws Exception {
        when(supplierQuoteService.searchCarretaQuotes("OC47")).thenReturn(SupplierQuoteSearchResponseDTO.builder()
                .query("OC47")
                .provider("CARRETA")
                .cached(false)
                .cachedAt(Instant.parse("2026-04-18T10:00:00Z"))
                .quotes(List.of(SupplierQuoteResponseDTO.builder()
                        .provider("CARRETA")
                        .articleNumber("OC47")
                        .brand("KNECHT/MAHLE")
                        .purchasePrice(new BigDecimal("152.02"))
                        .recommendedSalePrice(new BigDecimal("205.23"))
                        .quantityRaw("500")
                        .deliveryDaysMin(7)
                        .deliveryDaysMax(7)
                        .build()))
                .build());

        mockMvc.perform(get("/api/procurement/supplier-quotes/search")
                        .param("query", "OC47"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.provider").value("CARRETA"))
                .andExpect(jsonPath("$.quotes[0].purchasePrice").value(152.02))
                .andExpect(jsonPath("$.quotes[0].recommendedSalePrice").value(205.23));
    }
}
