package com.vladko.autoshopcore.integration.carreta.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class CarretaOrderResponse {

    private Integer id;
    private String number;
    private Integer status;

    @JsonProperty("status_display")
    private String statusDisplay;

    private String comment;

    @JsonProperty("client_comment")
    private String clientComment;

    private String created;
    private String code;
    private String maker;
    private String name;
    private String price;

    @JsonProperty("period_min")
    private Integer periodMin;

    @JsonProperty("period_max")
    private Integer periodMax;

    @JsonProperty("order_qty")
    private Integer orderQuantity;

    private String total;
}
