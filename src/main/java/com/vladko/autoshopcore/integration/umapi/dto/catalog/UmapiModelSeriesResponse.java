package com.vladko.autoshopcore.integration.umapi.dto.catalog;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class UmapiModelSeriesResponse {

    @JsonProperty("MFA_ID")
    private Integer manufacturerId;

    @JsonProperty("MANUFACTURER")
    private String manufacturer;

    @JsonProperty("MS_ID")
    private Integer modelSeriesId;

    @JsonProperty("MODEL_SERIES")
    private String modelSeries;

    @JsonProperty("CI_FROM")
    private String productionFrom;

    @JsonProperty("CI_TO")
    private String productionTo;

    @JsonProperty("TYPE")
    private String type;
}
