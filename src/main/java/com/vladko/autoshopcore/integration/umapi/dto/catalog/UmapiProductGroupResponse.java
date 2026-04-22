package com.vladko.autoshopcore.integration.umapi.dto.catalog;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class UmapiProductGroupResponse {

    @JsonProperty("PT_ID")
    private Integer productGroupId;

    @JsonProperty("DES")
    private String description;

    @JsonProperty("NORM_DES")
    private String normalizedDescription;
}
