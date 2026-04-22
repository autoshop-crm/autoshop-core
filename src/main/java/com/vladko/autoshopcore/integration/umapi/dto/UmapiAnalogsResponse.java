package com.vladko.autoshopcore.integration.umapi.dto;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class UmapiAnalogsResponse {

    private Integer rows;
    private Integer limit;
    private Integer offset;
    private List<UmapiArticleItem> data = new ArrayList<>();
}
