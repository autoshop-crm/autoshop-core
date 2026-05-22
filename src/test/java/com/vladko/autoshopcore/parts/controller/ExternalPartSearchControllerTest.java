package com.vladko.autoshopcore.parts.controller;

import com.vladko.autoshopcore.parts.dto.ExternalPartCatalogItemResponseDTO;
import com.vladko.autoshopcore.parts.dto.ExternalPartSearchResponseDTO;
import com.vladko.autoshopcore.parts.service.ExternalPartSearchService;
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

@WebMvcTest(ExternalPartSearchController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(GlobalExceptionHandler.class)
class ExternalPartSearchControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ExternalPartSearchService externalPartSearchService;

    @Test
    void searchShouldReturnUmapiCatalogItems() throws Exception {
        when(externalPartSearchService.search("OC90", "KNECHT", 10, 0)).thenReturn(ExternalPartSearchResponseDTO.builder()
                .articleNumber("OC90")
                .brand("KNECHT")
                .items(List.of(ExternalPartCatalogItemResponseDTO.builder()
                        .source("UMAPI_AUTOCATALOG")
                        .umapiArticleId(123)
                        .articleNumber("OC90")
                        .brand("KNECHT")
                        .name("Oil filter")
                        .build()))
                .build());

        mockMvc.perform(get("/api/parts/external/search")
                        .param("articleNumber", "OC90")
                        .param("brand", "KNECHT")
                        .param("limit", "10")
                        .param("offset", "0"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[0].source").value("UMAPI_AUTOCATALOG"))
                .andExpect(jsonPath("$.items[0].umapiArticleId").value(123));
    }
}
