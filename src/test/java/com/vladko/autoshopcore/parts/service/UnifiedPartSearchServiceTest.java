package com.vladko.autoshopcore.parts.service;

import com.vladko.autoshopcore.integration.umapi.support.UmapiArticleNormalizer;
import com.vladko.autoshopcore.parts.dto.ExternalPartCatalogItemResponseDTO;
import com.vladko.autoshopcore.parts.dto.ExternalPartSearchResponseDTO;
import com.vladko.autoshopcore.parts.dto.PartResponseDTO;
import com.vladko.autoshopcore.parts.dto.UnifiedPartSearchResponseDTO;
import com.vladko.autoshopcore.parts.entity.Part;
import com.vladko.autoshopcore.parts.repository.PartRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UnifiedPartSearchServiceTest {

    @Mock
    private PartService partService;
    @Mock
    private ExternalPartSearchService externalPartSearchService;
    @Mock
    private PartRepository partRepository;

    private UnifiedPartSearchService unifiedPartSearchService;

    @BeforeEach
    void setUp() {
        unifiedPartSearchService = new UnifiedPartSearchServiceImpl(
                partService,
                externalPartSearchService,
                partRepository,
                new UmapiArticleNormalizer()
        );
    }

    @Test
    void searchShouldReturnLocalAndExternalPartsWithMatchedWarehousePart() {
        PartResponseDTO localPart = PartResponseDTO.builder()
                .id(1)
                .brand("BOSCH")
                .name("Oil Filter")
                .articleNumber("OF123")
                .cost(BigDecimal.valueOf(550))
                .stockQuantity(5)
                .reservedQuantity(1)
                .availableQuantity(4)
                .createdAt(Instant.parse("2026-05-12T10:00:00Z"))
                .updatedAt(Instant.parse("2026-05-12T10:00:00Z"))
                .build();

        ExternalPartCatalogItemResponseDTO externalMatched = ExternalPartCatalogItemResponseDTO.builder()
                .source("UMAPI_AUTOCATALOG")
                .articleNumber("OF123")
                .brand("BOSCH")
                .name("Oil Filter External")
                .build();

        ExternalPartCatalogItemResponseDTO externalUnmatched = ExternalPartCatalogItemResponseDTO.builder()
                .source("UMAPI_AUTOCATALOG")
                .articleNumber("AF999")
                .brand("MANN")
                .name("Air Filter")
                .build();

        Part warehouseAnalog = Part.builder()
                .id(2)
                .brand("MANN")
                .name("Air Filter Local")
                .articleNumber("AF999")
                .cost(BigDecimal.valueOf(400))
                .stockQuantity(2)
                .reservedQuantity(0)
                .createdAt(Instant.parse("2026-05-12T10:00:00Z"))
                .updatedAt(Instant.parse("2026-05-12T10:00:00Z"))
                .build();

        when(partService.search("OF123", "BOSCH", null, true)).thenReturn(List.of(localPart));
        when(externalPartSearchService.search("OF123", "BOSCH", 10, 0)).thenReturn(ExternalPartSearchResponseDTO.builder()
                .articleNumber("OF123")
                .brand("BOSCH")
                .cached(true)
                .fallback(false)
                .items(List.of(externalMatched, externalUnmatched))
                .build());
        when(partRepository.findAllByArticleNumberIn(List.of("AF999"))).thenReturn(List.of(warehouseAnalog));

        UnifiedPartSearchResponseDTO response = unifiedPartSearchService.search("OF123", "BOSCH", true, 10, 0);

        assertThat(response.getItems()).hasSize(3);
        assertThat(response.getItems().get(0).getSourceType()).isEqualTo("LOCAL");
        assertThat(response.getItems().get(1).isExactLocalMatch()).isTrue();
        assertThat(response.getItems().get(1).getMatchedLocalPart().getId()).isEqualTo(1);
        assertThat(response.getItems().get(2).isExactLocalMatch()).isTrue();
        assertThat(response.getItems().get(2).getMatchedLocalPart().getArticleNumber()).isEqualTo("AF999");
    }
}
