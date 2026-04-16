package com.vladko.autoshopcore.order.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.vladko.autoshopcore.order.dto.OrderCreateDTO;
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
