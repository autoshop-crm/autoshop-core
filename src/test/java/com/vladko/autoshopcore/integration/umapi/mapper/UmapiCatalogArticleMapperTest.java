package com.vladko.autoshopcore.integration.umapi.mapper;

import com.vladko.autoshopcore.integration.umapi.dto.UmapiArticleItem;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class UmapiCatalogArticleMapperTest {

    private final UmapiCatalogArticleMapper mapper = new UmapiCatalogArticleMapper();

    @Test
    void mapShouldExposeArticleAndSupplierQuoteSearchUrl() {
        UmapiArticleItem item = new UmapiArticleItem();
        item.setArticleId(987);
        item.setArticleNumber("90915YZZE1");
        item.setSupplierId(111);
        item.setBrand("TOYOTA");
        item.setCompleteDescription("Oil filter");
        item.setOeCodes(List.of("90915YZZE1"));

        var dto = mapper.map(item);

        assertThat(dto.getSource()).isEqualTo("UMAPI_AUTOCATALOG");
        assertThat(dto.getArticleNumber()).isEqualTo("90915YZZE1");
        assertThat(dto.getOeCodes()).containsExactly("90915YZZE1");
        assertThat(dto.getSupplierQuoteSearchUrl())
                .isEqualTo("/api/procurement/supplier-quotes/search?query=90915YZZE1");
    }
}
