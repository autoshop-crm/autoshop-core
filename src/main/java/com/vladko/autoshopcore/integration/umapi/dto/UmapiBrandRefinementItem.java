package com.vladko.autoshopcore.integration.umapi.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class UmapiBrandRefinementItem {

    @JsonProperty("SEARCH_NUMBER")
    private String searchNumber;

    @JsonProperty("DISPLAY_NR")
    private String displayNumber;

    @JsonProperty("TYPE")
    private String type;

    @JsonProperty("BRA_ID")
    private Integer brandId;

    @JsonProperty("BRAND")
    private String brand;

    @JsonProperty("DES")
    private String description;

    @JsonProperty("TITLE")
    private String title;
}
