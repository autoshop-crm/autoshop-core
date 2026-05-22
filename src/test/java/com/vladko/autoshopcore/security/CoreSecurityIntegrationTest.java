package com.vladko.autoshopcore.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.vladko.autoshopcore.client.controller.CustomerController;
import com.vladko.autoshopcore.client.dto.CustomerCreateDTO;
import com.vladko.autoshopcore.client.service.CustomerService;
import com.vladko.autoshopcore.configuration.SecurityConfiguration;
import com.vladko.autoshopcore.order.controller.OrderController;
import com.vladko.autoshopcore.order.dto.OrderResponseDTO;
import com.vladko.autoshopcore.order.dto.OrderStatusUpdateDTO;
import com.vladko.autoshopcore.order.entity.OrderStatus;
import com.vladko.autoshopcore.order.service.OrderService;
import com.vladko.autoshopcore.procurement.controller.PurchaseOrderController;
import com.vladko.autoshopcore.procurement.dto.CarretaQuoteOrderDTO;
import com.vladko.autoshopcore.procurement.dto.PurchaseOrderCreateDTO;
import com.vladko.autoshopcore.procurement.dto.PurchaseOrderResponseDTO;
import com.vladko.autoshopcore.procurement.service.PurchaseOrderService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Set;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = {
        CustomerController.class,
        OrderController.class,
        PurchaseOrderController.class
})
@Import({
        SecurityConfiguration.class
})
class CoreSecurityIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private AuthServiceClient authServiceClient;

    @MockitoBean
    private CustomerService customerService;

    @MockitoBean
    private OrderService orderService;

    @MockitoBean
    private PurchaseOrderService purchaseOrderService;

    @Test
    void protectedEndpointWithoutTokenReturnsUnauthorized() throws Exception {
        mockMvc.perform(get("/api/customers/search"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401));
    }

    @Test
    void protectedEndpointWithInvalidTokenReturnsUnauthorized() throws Exception {
        when(authServiceClient.validateAccessToken("invalid-token"))
                .thenThrow(new InvalidAccessTokenException("Access token is invalid"));

        mockMvc.perform(get("/api/customers/search")
                        .header(HttpHeaders.AUTHORIZATION, bearer("invalid-token")))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Access token is invalid"));
    }

    @Test
    void protectedEndpointReturnsServiceUnavailableWhenAuthServiceIsDown() throws Exception {
        when(authServiceClient.validateAccessToken("manager-token"))
                .thenThrow(new AuthServiceUnavailableException("Authentication service is unavailable"));

        mockMvc.perform(get("/api/customers/search")
                        .header(HttpHeaders.AUTHORIZATION, bearer("manager-token")))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.status").value(503));
    }

    @Test
    void managerCanSearchCustomers() throws Exception {
        when(authServiceClient.validateAccessToken("manager-token"))
                .thenReturn(user("MANAGER"));
        when(customerService.search(isNull(), isNull(), isNull(), isNull())).thenReturn(List.of());

        mockMvc.perform(get("/api/customers/search")
                        .header(HttpHeaders.AUTHORIZATION, bearer("manager-token")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());
    }

    @Test
    void clientCannotCreateCustomerThroughStaffEndpoint() throws Exception {
        when(authServiceClient.validateAccessToken("client-token"))
                .thenReturn(user("CLIENT"));

        mockMvc.perform(post("/api/customers")
                        .header(HttpHeaders.AUTHORIZATION, bearer("client-token"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(CustomerCreateDTO.builder()
                                .firstName("Ivan")
                                .lastName("Petrov")
                                .phoneNumber("+79991234567")
                                .email("ivan@example.com")
                                .build())))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value("Access denied"));
    }

    @Test
    void mechanicCanUpdateOrderStatus() throws Exception {
        when(authServiceClient.validateAccessToken("mechanic-token"))
                .thenReturn(user("MECHANIC"));
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
                        .createdAt(Instant.parse("2026-04-21T09:00:00Z"))
                        .updatedAt(Instant.parse("2026-04-21T09:10:00Z"))
                        .build()
        );

        mockMvc.perform(put("/api/orders/10/status")
                        .header(HttpHeaders.AUTHORIZATION, bearer("mechanic-token"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                OrderStatusUpdateDTO.builder().status(OrderStatus.IN_PROGRESS).build()
                        )))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("IN_PROGRESS"));
    }

    @Test
    void mechanicCannotCreatePurchaseOrder() throws Exception {
        when(authServiceClient.validateAccessToken("mechanic-token"))
                .thenReturn(user("MECHANIC"));

        mockMvc.perform(post("/api/procurement/purchase-orders")
                        .header(HttpHeaders.AUTHORIZATION, bearer("mechanic-token"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validPurchaseOrder())))
                .andExpect(status().isForbidden());
    }

    @Test
    void mechanicCanGetMyOrders() throws Exception {
        when(authServiceClient.validateAccessToken("mechanic-token"))
                .thenReturn(user("MECHANIC"));
        when(orderService.getMyOrders()).thenReturn(java.util.List.of(
                OrderResponseDTO.builder()
                        .id(10)
                        .customerId(1)
                        .vehicleId(2)
                        .employeeId(1)
                        .problem("Diagnostics")
                        .status(OrderStatus.IN_PROGRESS)
                        .laborTotal(BigDecimal.ZERO)
                        .partsTotal(BigDecimal.ZERO)
                        .costsTotal(BigDecimal.ZERO)
                        .discountAmount(BigDecimal.ZERO)
                        .finalAmount(BigDecimal.ZERO)
                        .createdAt(Instant.parse("2026-04-21T09:00:00Z"))
                        .updatedAt(Instant.parse("2026-04-21T09:10:00Z"))
                        .build()
        ));

        mockMvc.perform(get("/api/orders/my")
                        .header(HttpHeaders.AUTHORIZATION, bearer("mechanic-token")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(10));
    }

    @Test
    void managerCanCreatePurchaseOrder() throws Exception {
        when(authServiceClient.validateAccessToken("manager-token"))
                .thenReturn(user("MANAGER"));
        when(purchaseOrderService.create(any(PurchaseOrderCreateDTO.class))).thenReturn(
                PurchaseOrderResponseDTO.builder()
                        .provider("CARRETA")
                        .articleNumber("OF-123")
                        .brand("Bosch")
                        .name("Oil filter")
                        .quantity(1)
                        .purchaseUnitPrice(new BigDecimal("10.00"))
                        .saleUnitPrice(new BigDecimal("15.00"))
                        .purchaseTotal(new BigDecimal("10.00"))
                        .saleTotal(new BigDecimal("15.00"))
                        .externalOrderCreated(false)
                        .testMode(true)
                        .build()
        );

        mockMvc.perform(post("/api/procurement/purchase-orders")
                        .header(HttpHeaders.AUTHORIZATION, bearer("manager-token"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validPurchaseOrder())))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.provider").value("CARRETA"));
    }

    private PurchaseOrderCreateDTO validPurchaseOrder() {
        return PurchaseOrderCreateDTO.builder()
                .quote(CarretaQuoteOrderDTO.builder()
                        .positionSignature("position-1")
                        .articleNumber("OF-123")
                        .brand("Bosch")
                        .name("Oil filter")
                        .purchasePrice(new BigDecimal("10.00"))
                        .deliveryDaysMin(1)
                        .deliveryDaysMax(2)
                        .minOrderQuantity(1)
                        .quantityRaw("10")
                        .build())
                .quantity(1)
                .salePrice(new BigDecimal("15.00"))
                .createExternalOrder(false)
                .build();
    }

    private AuthenticatedUser user(String role) {
        return new AuthenticatedUser(
                1L,
                role.toLowerCase() + "@autoshop.local",
                Set.of(role),
                "jti-" + role,
                Instant.parse("2026-04-21T09:15:00Z")
        );
    }

    private String bearer(String token) {
        return "Bearer " + token;
    }
}
