package com.vladko.autoshopcore.integration.umapi.dto.catalog;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class UmapiCategoryResponse {

    @JsonProperty("CATEGORY_ID")
    private Integer categoryId;

    @JsonProperty("PARENT_ID")
    private Integer parentId;

    @JsonProperty("DES")
    private String description;
}
