package com.vladko.autoshopcore.parts.service;

import com.vladko.autoshopcore.parts.dto.OrderPartItemCreateDTO;
import com.vladko.autoshopcore.parts.dto.OrderPartItemResponseDTO;
import com.vladko.autoshopcore.parts.dto.OrderPartItemUpdateDTO;

import java.util.List;

public interface OrderPartItemService {

    OrderPartItemResponseDTO create(Integer orderId, OrderPartItemCreateDTO dto);

    List<OrderPartItemResponseDTO> getAllByOrderId(Integer orderId);

    OrderPartItemResponseDTO update(Integer orderId, Integer itemId, OrderPartItemUpdateDTO dto);

    void delete(Integer orderId, Integer itemId);
}
