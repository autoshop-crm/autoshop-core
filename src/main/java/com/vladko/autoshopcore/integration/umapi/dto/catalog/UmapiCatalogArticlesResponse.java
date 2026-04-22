package com.vladko.autoshopcore.integration.umapi.dto.catalog;

import com.vladko.autoshopcore.integration.umapi.dto.UmapiArticleItem;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class UmapiCatalogArticlesResponse {

    private Integer rows;
    private Integer limit;
    private Integer offset;
    private List<UmapiArticleItem> data = new ArrayList<>();
}
