package com.vladko.autoshopcore.parts.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.vladko.autoshopcore.order.exception.OrderConflictException;
import com.vladko.autoshopcore.parts.dto.OrderPartItemCreateDTO;
import com.vladko.autoshopcore.parts.dto.OrderPartItemResponseDTO;
import com.vladko.autoshopcore.parts.dto.OrderPartItemUpdateDTO;
import com.vladko.autoshopcore.parts.exception.InsufficientPartStockException;
import com.vladko.autoshopcore.parts.exception.OrderPartItemNotFoundException;
import com.vladko.autoshopcore.parts.service.OrderPartItemService;
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
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(OrderPartItemController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(GlobalExceptionHandler.class)
class OrderPartItemControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private OrderPartItemService orderPartItemService;

    @Test
    void createShouldReturnCreatedOrderPartItem() throws Exception {
        when(orderPartItemService.create(any(Integer.class), any(OrderPartItemCreateDTO.class))).thenReturn(
                OrderPartItemResponseDTO.builder()
                        .id(15)
                        .orderId(10)
                        .partId(5)
                        .articleNumber("OF-123")
                        .brand("Bosch")
                        .name("Oil filter")
                        .quantity(2)
                        .unitPrice(new BigDecimal("15.50"))
                        .lineTotal(new BigDecimal("31.00"))
                        .build()
        );

        mockMvc.perform(post("/api/orders/10/parts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new OrderPartItemCreateDTO(5, 2))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(15))
                .andExpect(jsonPath("$.lineTotal").value(31.00));
    }

    @Test
    void createShouldReturnBadRequestForInvalidPayload() throws Exception {
        mockMvc.perform(post("/api/orders/10/parts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new OrderPartItemCreateDTO(null, 0))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));
    }

    @Test
    void createShouldReturnConflictWhenStockInsufficient() throws Exception {
        when(orderPartItemService.create(any(Integer.class), any(OrderPartItemCreateDTO.class)))
                .thenThrow(new InsufficientPartStockException("Part with id '5' does not have enough available stock"));

        mockMvc.perform(post("/api/orders/10/parts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new OrderPartItemCreateDTO(5, 3))))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("Part with id '5' does not have enough available stock"));
    }

    @Test
    void getAllByOrderIdShouldReturnItems() throws Exception {
        when(orderPartItemService.getAllByOrderId(10)).thenReturn(List.of(
                OrderPartItemResponseDTO.builder()
                        .id(15)
                        .orderId(10)
                        .partId(5)
                        .articleNumber("OF-123")
                        .brand("Bosch")
                        .name("Oil filter")
                        .quantity(2)
                        .unitPrice(new BigDecimal("15.50"))
                        .lineTotal(new BigDecimal("31.00"))
                        .build()
        ));

        mockMvc.perform(get("/api/orders/10/parts"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].partId").value(5));
    }

    @Test
    void updateShouldReturnConflictForDuplicatePartState() throws Exception {
        when(orderPartItemService.update(any(Integer.class), any(Integer.class), any(OrderPartItemUpdateDTO.class)))
                .thenThrow(new OrderConflictException("Part is already reserved for this order"));

        mockMvc.perform(put("/api/orders/10/parts/15")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new OrderPartItemUpdateDTO(2))))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("Part is already reserved for this order"));
    }

    @Test
    void deleteShouldReturnNotFoundWhenItemMissing() throws Exception {
        doThrow(new OrderPartItemNotFoundException(10, 15))
                .when(orderPartItemService).delete(10, 15);

        mockMvc.perform(delete("/api/orders/10/parts/15"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Order part item with id '15' was not found for order '10'"));
    }
}
