package com.vladko.autoshopcore.parts.service.catalog;

import com.vladko.autoshopcore.parts.dto.catalog.CatalogArticleSearchResponseDTO;
import com.vladko.autoshopcore.parts.dto.catalog.CatalogProductGroupSearchResponseDTO;

import java.util.List;

public interface PartCatalogSearchService {

    CatalogProductGroupSearchResponseDTO searchProductGroups(String type, Integer modificationId, String query);

    CatalogArticleSearchResponseDTO searchArticles(String type,
                                                   Integer modificationId,
                                                   List<Integer> productGroupIds,
                                                   Integer supplierId,
                                                   Integer limit,
                                                   Integer offset);
}
