package com.vladko.autoshopcore.integration.umapi.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class UmapiArticleItem {

    @JsonProperty("ART_ID")
    private Integer articleId;

    @JsonProperty("ARTICLE_NR")
    private String articleNumber;

    @JsonProperty("SUP_ID")
    private Integer supplierId;

    @JsonProperty("BRAND")
    private String brand;

    @JsonProperty("COMPLETE_DES")
    private String completeDescription;

    @JsonProperty("DES")
    private String description;

    @JsonProperty("STATUS_DES")
    private String statusDescription;

    @JsonProperty("MEDIA_FILE")
    private String mediaFile;

    @JsonFormat(with = JsonFormat.Feature.ACCEPT_SINGLE_VALUE_AS_ARRAY)
    @JsonProperty("OE_CODES")
    private List<String> oeCodes = new ArrayList<>();

    @JsonFormat(with = JsonFormat.Feature.ACCEPT_SINGLE_VALUE_AS_ARRAY)
    @JsonProperty("EAN_CODES")
    private List<String> eanCodes = new ArrayList<>();

    @JsonProperty("ARL_DISPLAY_NR")
    private String linkedDisplayNumber;

    @JsonProperty("ARL_BRA_BRAND")
    private String linkedBrand;

    @JsonProperty("ARL_TYPE")
    private String linkedType;
}
