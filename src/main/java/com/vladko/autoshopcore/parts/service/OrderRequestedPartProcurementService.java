package com.vladko.autoshopcore.parts.service;

import com.vladko.autoshopcore.parts.dto.OrderRequestedPartOrderDTO;
import com.vladko.autoshopcore.parts.dto.OrderRequestedPartResponseDTO;

public interface OrderRequestedPartProcurementService {
    OrderRequestedPartResponseDTO order(Integer orderId, Integer requestedPartId, OrderRequestedPartOrderDTO dto);
}
