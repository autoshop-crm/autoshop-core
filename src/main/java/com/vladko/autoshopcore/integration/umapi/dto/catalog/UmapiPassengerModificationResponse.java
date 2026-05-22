package com.vladko.autoshopcore.integration.umapi.dto.catalog;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class UmapiPassengerModificationResponse {

    @JsonProperty("PC_ID")
    private Integer modificationId;

    @JsonProperty("PASSENGER_CAR")
    private String name;

    @JsonProperty("POWER_PS")
    private BigDecimal powerPs;

    @JsonProperty("CAPACITY_LT")
    private BigDecimal capacityLiters;

    @JsonProperty("ENGINE_TYPE")
    private String engineType;

    @JsonProperty("BODY_TYPE")
    private String bodyType;

    @JsonProperty("FUEL_TYPE")
    private String fuelType;
}
