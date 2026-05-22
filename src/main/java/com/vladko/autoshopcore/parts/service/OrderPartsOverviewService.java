package com.vladko.autoshopcore.parts.service;

import com.vladko.autoshopcore.parts.dto.OrderPartsOverviewResponseDTO;

public interface OrderPartsOverviewService {
    OrderPartsOverviewResponseDTO getOverview(Integer orderId);
}
