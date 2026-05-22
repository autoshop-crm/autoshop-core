package com.vladko.autoshopcore.integration.umapi.dto.catalog;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class UmapiManufacturerResponse {

    @JsonProperty("MFA_ID")
    private Integer mfaId;

    @JsonProperty("MANUFACTURER")
    private String manufacturer;
}
