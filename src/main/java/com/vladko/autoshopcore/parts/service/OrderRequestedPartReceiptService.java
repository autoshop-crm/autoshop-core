package com.vladko.autoshopcore.parts.service;

import com.vladko.autoshopcore.parts.dto.OrderRequestedPartReceiveDTO;
import com.vladko.autoshopcore.parts.dto.OrderRequestedPartResponseDTO;

public interface OrderRequestedPartReceiptService {
    OrderRequestedPartResponseDTO receive(Integer orderId, Integer requestedPartId, OrderRequestedPartReceiveDTO dto);
}
