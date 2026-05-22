package com.vladko.autoshopcore.order.service;

import com.vladko.autoshopcore.order.dto.OrderAssignmentDTO;
import com.vladko.autoshopcore.order.dto.OrderCreateDTO;
import com.vladko.autoshopcore.order.dto.OrderEstimateUpdateDTO;
import com.vladko.autoshopcore.order.dto.OrderResponseDTO;
import com.vladko.autoshopcore.order.dto.OrderStatusUpdateDTO;
import com.vladko.autoshopcore.order.dto.OrderUpdateDTO;
import com.vladko.autoshopcore.order.entity.OrderStatus;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

public interface OrderService {

    OrderResponseDTO create(OrderCreateDTO dto);

    OrderResponseDTO createForCustomer(OrderCreateDTO dto);

    OrderResponseDTO createImmediateDropOff(OrderCreateDTO dto);

    OrderResponseDTO getById(Integer id);

    OrderResponseDTO update(Integer id, OrderUpdateDTO dto);

    OrderResponseDTO updateForCustomer(Integer id, OrderUpdateDTO dto);

    OrderResponseDTO assignEmployee(Integer id, OrderAssignmentDTO dto);

    OrderResponseDTO updateEstimate(Integer id, OrderEstimateUpdateDTO dto);

    OrderResponseDTO updateStatus(Integer id, OrderStatusUpdateDTO dto);

    OrderResponseDTO checkInVehicle(Integer id);

    OrderResponseDTO cancelNoShow(Integer id);

    List<OrderResponseDTO> getAll();

    List<OrderResponseDTO> getAllByCustomerId(Integer customerId);

    List<OrderResponseDTO> getAllByVehicleId(Integer vehicleId);

    List<OrderResponseDTO> getMyOrders();

    List<OrderResponseDTO> getAllByStatus(OrderStatus status);

    List<OrderResponseDTO> getBookings(Instant from, Instant to);

    List<OrderResponseDTO> getDailyArrivals(LocalDate date);

    List<OrderResponseDTO> getUnassignedBookings(LocalDate date);
}
