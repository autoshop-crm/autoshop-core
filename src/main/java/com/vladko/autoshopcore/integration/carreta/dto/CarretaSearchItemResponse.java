package com.vladko.autoshopcore.integration.carreta.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class CarretaSearchItemResponse {

    @JsonProperty("_")
    private String positionSignature;

    private String code;
    private String desc;
    private Boolean isCross;
    private String maker;
    private Integer minQty;
    private String name;
    private Integer periodMax;
    private Integer periodMin;
    private String price;
    private String qty;
    private Integer stat;
    private String source;

    @JsonProperty("is_cross")
    public void setIsCross(Boolean isCross) {
        this.isCross = isCross;
    }

    @JsonProperty("min_qty")
    public void setMinQty(Integer minQty) {
        this.minQty = minQty;
    }

    @JsonProperty("period_max")
    public void setPeriodMax(Integer periodMax) {
        this.periodMax = periodMax;
    }

    @JsonProperty("period_min")
    public void setPeriodMin(Integer periodMin) {
        this.periodMin = periodMin;
    }
}
