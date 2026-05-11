package com.vladko.autoshopcore.parts.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.vladko.autoshopcore.parts.dto.OrderRequestedPartOrderDTO;
import com.vladko.autoshopcore.parts.dto.OrderRequestedPartReceiveDTO;
import com.vladko.autoshopcore.parts.dto.OrderRequestedPartResponseDTO;
import com.vladko.autoshopcore.parts.entity.OrderRequestedPartStatus;
import com.vladko.autoshopcore.parts.service.OrderRequestedPartProcurementService;
import com.vladko.autoshopcore.parts.service.OrderRequestedPartReceiptService;
import com.vladko.autoshopcore.procurement.dto.CarretaQuoteOrderDTO;
import com.vladko.autoshopcore.shared.exception.GlobalExceptionHandler;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(OrderRequestedPartProcurementController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(GlobalExceptionHandler.class)
class OrderRequestedPartProcurementControllerTest {
    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;

    @MockitoBean OrderRequestedPartProcurementService procurementService;
    @MockitoBean OrderRequestedPartReceiptService receiptService;

    @Test
    void orderShouldReturnUpdatedRequestedPart() throws Exception {
        when(procurementService.order(any(), any(), any())).thenReturn(OrderRequestedPartResponseDTO.builder().status(OrderRequestedPartStatus.ORDERED_IN_TRANSIT).salePrice(BigDecimal.valueOf(150)).build());
        OrderRequestedPartOrderDTO dto = OrderRequestedPartOrderDTO.builder()
                .quote(CarretaQuoteOrderDTO.builder().positionSignature("sig").articleNumber("OF123").brand("BOSCH").name("Oil Filter").purchasePrice(BigDecimal.TEN).deliveryDaysMin(1).deliveryDaysMax(2).minOrderQuantity(1).quantityRaw("10").build())
                .salePrice(BigDecimal.valueOf(150))
                .build();
        mockMvc.perform(post("/api/orders/3/requested-parts/11/order").contentType(APPLICATION_JSON).content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ORDERED_IN_TRANSIT"));
    }

    @Test
    void receiveShouldReturnReservedRequestedPart() throws Exception {
        when(receiptService.receive(any(), any(), any())).thenReturn(OrderRequestedPartResponseDTO.builder().status(OrderRequestedPartStatus.IN_STOCK_RESERVED).build());
        mockMvc.perform(post("/api/orders/3/requested-parts/11/receive").contentType(APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(OrderRequestedPartReceiveDTO.builder().targetPartId(7).receivedQuantity(2).build())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("IN_STOCK_RESERVED"));
    }
}
