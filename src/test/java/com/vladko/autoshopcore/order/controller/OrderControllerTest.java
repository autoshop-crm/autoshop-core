package com.vladko.autoshopcore.order.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.vladko.autoshopcore.order.dto.OrderAssignmentDTO;
import com.vladko.autoshopcore.order.dto.OrderCreateDTO;
import com.vladko.autoshopcore.order.dto.OrderEstimateUpdateDTO;
import com.vladko.autoshopcore.order.dto.OrderResponseDTO;
import com.vladko.autoshopcore.order.dto.OrderStatusUpdateDTO;
import com.vladko.autoshopcore.order.entity.OrderStatus;
import com.vladko.autoshopcore.order.exception.EmployeeNotFoundException;
import com.vladko.autoshopcore.order.exception.InvalidOrderStateException;
import com.vladko.autoshopcore.order.exception.OrderConflictException;
import com.vladko.autoshopcore.order.exception.OrderNotFoundException;
import com.vladko.autoshopcore.order.service.OrderService;
import com.vladko.autoshopcore.shared.exception.GlobalExceptionHandler;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.Instant;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(OrderController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(GlobalExceptionHandler.class)
class OrderControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private OrderService orderService;

    @Test
    void createShouldReturnCreatedOrder() throws Exception {
        OrderCreateDTO dto = OrderCreateDTO.builder()
                .customerId(1)
                .vehicleId(2)
                .problem("Diagnostics")
                .build();

        when(orderService.create(any(OrderCreateDTO.class))).thenReturn(OrderResponseDTO.builder()
                .id(15)
                .customerId(1)
                .vehicleId(2)
                .problem("Diagnostics")
                .status(OrderStatus.NEW)
                .laborTotal(BigDecimal.ZERO)
                .partsTotal(BigDecimal.ZERO)
                .costsTotal(BigDecimal.ZERO)
                .discountAmount(BigDecimal.ZERO)
                .finalAmount(BigDecimal.ZERO)
                .createdAt(Instant.parse("2026-04-14T10:15:30Z"))
                .updatedAt(Instant.parse("2026-04-14T10:15:30Z"))
                .build());

        mockMvc.perform(post("/api/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(15))
                .andExpect(jsonPath("$.status").value("NEW"));
    }

    @Test
    void createShouldReturnBadRequestForInvalidPayload() throws Exception {
        OrderCreateDTO dto = OrderCreateDTO.builder()
                .customerId(null)
                .vehicleId(null)
                .problem("")
                .build();

        mockMvc.perform(post("/api/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));
    }

    @Test
    void getByIdShouldReturnNotFound() throws Exception {
        when(orderService.getById(404)).thenThrow(new OrderNotFoundException(404));

        mockMvc.perform(get("/api/orders/404"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Order with id '404' was not found"));
    }

    @Test
    void createShouldReturnConflictForCustomerVehicleMismatch() throws Exception {
        OrderCreateDTO dto = OrderCreateDTO.builder()
                .customerId(1)
                .vehicleId(2)
                .problem("Diagnostics")
                .build();

        when(orderService.create(any(OrderCreateDTO.class)))
                .thenThrow(new OrderConflictException("Vehicle does not belong to the specified customer"));

        mockMvc.perform(post("/api/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("Vehicle does not belong to the specified customer"));
    }

    @Test
    void createShouldReturnNotFoundWhenEmployeeMissing() throws Exception {
        OrderCreateDTO dto = OrderCreateDTO.builder()
                .customerId(1)
                .vehicleId(2)
                .employeeId(99)
                .problem("Diagnostics")
                .build();

        when(orderService.create(any(OrderCreateDTO.class)))
                .thenThrow(new EmployeeNotFoundException(99));

        mockMvc.perform(post("/api/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Employee with id '99' was not found"));
    }

    @Test
    void assignEmployeeShouldReturnUpdatedOrder() throws Exception {
        when(orderService.assignEmployee(any(Integer.class), any(OrderAssignmentDTO.class))).thenReturn(
                OrderResponseDTO.builder()
                        .id(10)
                        .customerId(1)
                        .vehicleId(2)
                        .employeeId(21)
                        .problem("Diagnostics")
                        .status(OrderStatus.NEW)
                        .laborTotal(BigDecimal.ZERO)
                        .partsTotal(BigDecimal.ZERO)
                        .costsTotal(BigDecimal.ZERO)
                        .discountAmount(BigDecimal.ZERO)
                        .finalAmount(BigDecimal.ZERO)
                        .createdAt(Instant.parse("2026-04-14T10:15:30Z"))
                        .updatedAt(Instant.parse("2026-04-14T10:20:30Z"))
                        .build()
        );

        mockMvc.perform(put("/api/orders/10/assign")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                OrderAssignmentDTO.builder().employeeId(21).build()
                        )))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(10))
                .andExpect(jsonPath("$.employeeId").value(21));
    }

    @Test
    void assignEmployeeShouldReturnConflictForInvalidAssignment() throws Exception {
        when(orderService.assignEmployee(any(Integer.class), any(OrderAssignmentDTO.class)))
                .thenThrow(new OrderConflictException("Only mechanic or manager can be assigned to order"));

        mockMvc.perform(put("/api/orders/10/assign")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                OrderAssignmentDTO.builder().employeeId(21).build()
                        )))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("Only mechanic or manager can be assigned to order"));
    }

    @Test
    void updateEstimateShouldReturnUpdatedOrder() throws Exception {
        when(orderService.updateEstimate(any(Integer.class), any(OrderEstimateUpdateDTO.class))).thenReturn(
                OrderResponseDTO.builder()
                        .id(10)
                        .customerId(1)
                        .vehicleId(2)
                        .problem("Diagnostics")
                        .status(OrderStatus.NEW)
                        .laborTotal(new BigDecimal("100.00"))
                        .partsTotal(BigDecimal.ZERO)
                        .costsTotal(new BigDecimal("100.00"))
                        .discountAmount(new BigDecimal("15.00"))
                        .finalAmount(new BigDecimal("85.00"))
                        .createdAt(Instant.parse("2026-04-14T10:15:30Z"))
                        .updatedAt(Instant.parse("2026-04-14T10:20:30Z"))
                        .build()
        );

        mockMvc.perform(put("/api/orders/10/estimate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                OrderEstimateUpdateDTO.builder()
                                        .laborTotal(new BigDecimal("100.00"))
                                        .discountAmount(new BigDecimal("15.00"))
                                        .build()
                        )))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.laborTotal").value(100.00))
                .andExpect(jsonPath("$.finalAmount").value(85.00));
    }

    @Test
    void updateEstimateShouldReturnBadRequestForNegativeValues() throws Exception {
        mockMvc.perform(put("/api/orders/10/estimate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                OrderEstimateUpdateDTO.builder()
                                        .laborTotal(new BigDecimal("-1.00"))
                                        .discountAmount(BigDecimal.ZERO)
                                        .build()
                        )))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));
    }

    @Test
    void updateEstimateShouldReturnConflictForInvalidEstimate() throws Exception {
        when(orderService.updateEstimate(any(Integer.class), any(OrderEstimateUpdateDTO.class)))
                .thenThrow(new OrderConflictException("Discount amount cannot exceed total costs"));

        mockMvc.perform(put("/api/orders/10/estimate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                OrderEstimateUpdateDTO.builder()
                                        .laborTotal(new BigDecimal("100.00"))
                                        .discountAmount(new BigDecimal("120.00"))
                                        .build()
                        )))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("Discount amount cannot exceed total costs"));
    }

    @Test
    void updateStatusShouldReturnConflictForInvalidTransition() throws Exception {
        when(orderService.updateStatus(any(Integer.class), any(OrderStatusUpdateDTO.class)))
                .thenThrow(new InvalidOrderStateException("Cannot transition order status from 'NEW' to 'COMPLETED'"));

        mockMvc.perform(put("/api/orders/10/status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                OrderStatusUpdateDTO.builder().status(OrderStatus.COMPLETED).build()
                        )))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("Cannot transition order status from 'NEW' to 'COMPLETED'"));
    }

    @Test
    void updateStatusShouldReturnUpdatedOrder() throws Exception {
        when(orderService.updateStatus(any(Integer.class), any(OrderStatusUpdateDTO.class))).thenReturn(
                OrderResponseDTO.builder()
                        .id(10)
                        .customerId(1)
                        .vehicleId(2)
                        .problem("Diagnostics")
                        .status(OrderStatus.IN_PROGRESS)
                        .laborTotal(BigDecimal.ZERO)
                        .partsTotal(BigDecimal.ZERO)
                        .costsTotal(BigDecimal.ZERO)
                        .discountAmount(BigDecimal.ZERO)
                        .finalAmount(BigDecimal.ZERO)
                        .createdAt(Instant.parse("2026-04-14T10:15:30Z"))
                        .updatedAt(Instant.parse("2026-04-14T10:20:30Z"))
                        .build()
        );

        mockMvc.perform(put("/api/orders/10/status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                OrderStatusUpdateDTO.builder().status(OrderStatus.IN_PROGRESS).build()
                        )))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(10))
                .andExpect(jsonPath("$.status").value("IN_PROGRESS"));
    }
}
