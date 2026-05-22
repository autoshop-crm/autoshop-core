package com.vladko.autoshopcore.integration.umapi.dto.catalog;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class UmapiCategoriesResponse {

    private List<UmapiCategoryResponse> data = new ArrayList<>();
}
