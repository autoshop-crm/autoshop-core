package com.vladko.autoshopcore.integration.carreta.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class CarretaOrderCreateRequest {

    @JsonProperty("_")
    private String positionSignature;

    private String code;
    private String maker;
    private String name;
    private String price;

    @JsonProperty("period_min")
    private Integer periodMin;

    @JsonProperty("period_max")
    private Integer periodMax;

    @JsonProperty("min_qty")
    private Integer minQty;

    private String qty;

    @JsonProperty("order_qty")
    private Integer orderQuantity;

    @JsonProperty("client_comment")
    private String clientComment;
}
