package com.vladko.autoshopcore.parts.controller;

import com.vladko.autoshopcore.parts.dto.UnifiedPartSearchItemResponseDTO;
import com.vladko.autoshopcore.parts.dto.UnifiedPartSearchResponseDTO;
import com.vladko.autoshopcore.parts.service.UnifiedPartSearchService;
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

@WebMvcTest(UnifiedPartSearchController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(GlobalExceptionHandler.class)
class UnifiedPartSearchControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private UnifiedPartSearchService unifiedPartSearchService;

    @Test
    void searchShouldReturnUnifiedItems() throws Exception {
        when(unifiedPartSearchService.search("OF123", "BOSCH", true, 10, 0)).thenReturn(
                UnifiedPartSearchResponseDTO.builder()
                        .articleNumber("OF123")
                        .brand("BOSCH")
                        .items(List.of(
                                UnifiedPartSearchItemResponseDTO.builder()
                                        .sourceType("LOCAL")
                                        .articleNumber("OF123")
                                        .build(),
                                UnifiedPartSearchItemResponseDTO.builder()
                                        .sourceType("EXTERNAL")
                                        .articleNumber("AF999")
                                        .exactLocalMatch(true)
                                        .build()
                        ))
                        .build()
        );

        mockMvc.perform(get("/api/parts/unified/search")
                        .param("articleNumber", "OF123")
                        .param("brand", "BOSCH")
                        .param("availableOnly", "true")
                        .param("limit", "10")
                        .param("offset", "0"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[0].sourceType").value("LOCAL"))
                .andExpect(jsonPath("$.items[1].sourceType").value("EXTERNAL"))
                .andExpect(jsonPath("$.items[1].exactLocalMatch").value(true));
    }
}
