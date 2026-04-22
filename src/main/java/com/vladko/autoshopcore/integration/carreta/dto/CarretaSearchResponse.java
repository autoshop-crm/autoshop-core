package com.vladko.autoshopcore.integration.carreta.dto;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class CarretaSearchResponse {

    private List<CarretaSearchItemResponse> objects = new ArrayList<>();
}
