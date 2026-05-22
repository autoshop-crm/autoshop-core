package com.vladko.autoshopcore.parts.controller;

import com.vladko.autoshopcore.parts.dto.catalog.CatalogArticleResponseDTO;
import com.vladko.autoshopcore.parts.dto.catalog.CatalogArticleSearchResponseDTO;
import com.vladko.autoshopcore.parts.dto.catalog.CatalogManufacturerResponseDTO;
import com.vladko.autoshopcore.parts.dto.catalog.CatalogProductGroupResponseDTO;
import com.vladko.autoshopcore.parts.dto.catalog.CatalogProductGroupSearchResponseDTO;
import com.vladko.autoshopcore.parts.service.catalog.PartCatalogSearchService;
import com.vladko.autoshopcore.parts.service.catalog.VehicleCatalogLookupService;
import com.vladko.autoshopcore.shared.exception.GlobalExceptionHandler;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.List;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(CatalogPartSearchController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(GlobalExceptionHandler.class)
class CatalogPartSearchControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private VehicleCatalogLookupService vehicleCatalogLookupService;

    @MockitoBean
    private PartCatalogSearchService partCatalogSearchService;

    @Test
    void getManufacturersShouldReturnCatalogManufacturers() throws Exception {
        when(vehicleCatalogLookupService.getManufacturers("PC", true)).thenReturn(List.of(
                CatalogManufacturerResponseDTO.builder()
                        .type("PC")
                        .manufacturerId(111)
                        .name("TOYOTA")
                        .build()
        ));

        mockMvc.perform(get("/api/parts/catalog/manufacturers")
                        .param("type", "PC")
                        .param("popular", "true"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].manufacturerId").value(111))
                .andExpect(jsonPath("$[0].name").value("TOYOTA"));
    }

    @Test
    void searchProductGroupsShouldReturnScoredItems() throws Exception {
        when(partCatalogSearchService.searchProductGroups("PC", 333, "масляный фильтр"))
                .thenReturn(CatalogProductGroupSearchResponseDTO.builder()
                        .type("PC")
                        .modificationId(333)
                        .query("масляный фильтр")
                        .items(List.of(CatalogProductGroupResponseDTO.builder()
                                .productGroupId(7)
                                .name("Масляный фильтр")
                                .score(new BigDecimal("1.00"))
                                .build()))
                        .build());

        mockMvc.perform(get("/api/parts/catalog/product-groups/search")
                        .param("type", "PC")
                        .param("modificationId", "333")
                        .param("query", "масляный фильтр"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[0].productGroupId").value(7));
    }

    @Test
    void searchArticlesShouldReturnCatalogArticles() throws Exception {
        when(partCatalogSearchService.searchArticles("PC", 333, List.of(7), null, 10, 0))
                .thenReturn(CatalogArticleSearchResponseDTO.builder()
                        .type("PC")
                        .modificationId(333)
                        .productGroupIds(List.of(7))
                        .items(List.of(CatalogArticleResponseDTO.builder()
                                .umapiArticleId(987)
                                .articleNumber("90915YZZE1")
                                .brand("TOYOTA")
                                .build()))
                        .build());

        mockMvc.perform(get("/api/parts/catalog/articles")
                        .param("type", "PC")
                        .param("modificationId", "333")
                        .param("productGroupIds", "7")
                        .param("limit", "10")
                        .param("offset", "0"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[0].umapiArticleId").value(987))
                .andExpect(jsonPath("$.items[0].articleNumber").value("90915YZZE1"));
    }
}
