package com.vladko.autoshopcore.integration.umapi.client;

import com.vladko.autoshopcore.integration.umapi.dto.UmapiAnalogsResponse;
import com.vladko.autoshopcore.integration.umapi.dto.UmapiArticleItem;
import com.vladko.autoshopcore.integration.umapi.dto.UmapiBrandRefinementItem;
import com.vladko.autoshopcore.integration.umapi.dto.catalog.UmapiFuseProductGroupResponse;
import com.vladko.autoshopcore.integration.umapi.dto.catalog.UmapiManufacturerResponse;
import com.vladko.autoshopcore.integration.umapi.dto.catalog.UmapiModelSeriesResponse;
import com.vladko.autoshopcore.integration.umapi.dto.catalog.UmapiPassengerModificationResponse;

import java.util.List;

public interface UmapiClient {

    List<UmapiBrandRefinementItem> refineBrand(String articleNumber);

    UmapiAnalogsResponse findAnalogs(String articleNumber, String brand, int limit, int offset);

    UmapiArticleItem getArticle(Integer articleId);

    List<UmapiManufacturerResponse> getManufacturers(String type, boolean popular);

    List<UmapiModelSeriesResponse> getModelSeries(String type, Integer manufacturerId);

    List<UmapiPassengerModificationResponse> getPassengerModifications(String type, Integer modelSeriesId);

    List<UmapiFuseProductGroupResponse> getFuseProductGroups(String type, Integer modificationId);

    List<UmapiArticleItem> getArticles(
            String type,
            List<Integer> productGroupIds,
            Integer modificationId,
            Integer supplierId,
            int limit,
            int offset
    );
}
