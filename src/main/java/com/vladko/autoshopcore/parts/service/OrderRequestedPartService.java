package com.vladko.autoshopcore.parts.service;

import com.vladko.autoshopcore.parts.dto.OrderRequestedPartCreateDTO;
import com.vladko.autoshopcore.parts.dto.OrderRequestedPartResponseDTO;

import java.util.List;

public interface OrderRequestedPartService {
    OrderRequestedPartResponseDTO create(Integer orderId, OrderRequestedPartCreateDTO dto);
    List<OrderRequestedPartResponseDTO> getAllByOrderId(Integer orderId);
}
