package com.vladko.autoshopcore.order.service;

import com.vladko.autoshopcore.order.dto.OrderCreateDTO;
import com.vladko.autoshopcore.order.dto.OrderResponseDTO;
import com.vladko.autoshopcore.order.dto.OrderStatusUpdateDTO;
import com.vladko.autoshopcore.order.dto.OrderUpdateDTO;
import com.vladko.autoshopcore.order.entity.OrderStatus;

import java.util.List;

public interface OrderService {

    OrderResponseDTO create(OrderCreateDTO dto);

    OrderResponseDTO getById(Integer id);

    OrderResponseDTO update(Integer id, OrderUpdateDTO dto);

    OrderResponseDTO updateStatus(Integer id, OrderStatusUpdateDTO dto);

    List<OrderResponseDTO> getAllByCustomerId(Integer customerId);

    List<OrderResponseDTO> getAllByVehicleId(Integer vehicleId);

    List<OrderResponseDTO> getAllByStatus(OrderStatus status);
}
