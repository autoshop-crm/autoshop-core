package com.vladko.autoshopcore.integration.carreta.client;

import com.vladko.autoshopcore.integration.carreta.dto.CarretaOrderCreateRequest;
import com.vladko.autoshopcore.integration.carreta.dto.CarretaOrderResponse;
import com.vladko.autoshopcore.integration.carreta.dto.CarretaProfileResponse;
import com.vladko.autoshopcore.integration.carreta.dto.CarretaSearchResponse;

public interface CarretaClient {

    CarretaSearchResponse search(String query);

    CarretaOrderResponse createOrder(CarretaOrderCreateRequest request, boolean testMode);

    CarretaOrderResponse getOrder(Integer externalOrderId);

    CarretaProfileResponse getProfile();
}
