package com.vladko.autoshopcore.client.service;

import com.vladko.autoshopcore.client.dto.CustomerUpdateDTO;
import com.vladko.autoshopcore.client.entity.Customer;
import com.vladko.autoshopcore.client.repository.CustomerRepository;
import com.vladko.autoshopcore.customerauth.service.CustomerIdentityLinkService;
import com.vladko.autoshopcore.loyalty.service.CrmLoyaltyFacade;
import com.vladko.autoshopcore.loyalty.service.LoyaltyService;
import com.vladko.autoshopcore.order.approval.service.OrderApprovalService;
import com.vladko.autoshopcore.order.dto.OrderResponseDTO;
import com.vladko.autoshopcore.order.service.OrderService;
import com.vladko.autoshopcore.security.AuthenticatedUser;
import com.vladko.autoshopcore.vehicle.dto.VehicleResponseDTO;
import com.vladko.autoshopcore.vehicle.service.VehicleService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.time.Instant;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CustomerSelfServiceTest {

    @Mock private CustomerRepository customerRepository;
    @Mock private CustomerIdentityLinkService customerIdentityLinkService;
    @Mock private OrderService orderService;
    @Mock private VehicleService vehicleService;
    @Mock private LoyaltyService loyaltyService;
    @Mock private CrmLoyaltyFacade crmLoyaltyFacade;
    @Mock private OrderApprovalService orderApprovalService;

    @InjectMocks private CustomerSelfServiceImpl customerSelfService;

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void getCurrentCustomerOrdersShouldUseResolvedCustomer() {
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(
                new AuthenticatedUser(44L, "client@test.com", Set.of("CUSTOMER"), "jti", Instant.parse("2026-05-20T10:00:00Z")),
                null,
                java.util.List.of()
        ));
        when(customerIdentityLinkService.getRequiredCurrentCustomer(44L, "client@test.com"))
                .thenReturn(Customer.builder().id(9).email("client@test.com").build());
        when(orderService.getAllByCustomerId(9)).thenReturn(List.of(OrderResponseDTO.builder().id(100).build()));

        List<OrderResponseDTO> response = customerSelfService.getCurrentCustomerOrders();

        assertThat(response).extracting(OrderResponseDTO::getId).containsExactly(100);
    }

    @Test
    void getCurrentCustomerVehiclesShouldUseResolvedCustomer() {
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(
                new AuthenticatedUser(44L, "client@test.com", Set.of("CUSTOMER"), "jti", Instant.parse("2026-05-20T10:00:00Z")),
                null,
                java.util.List.of()
        ));
        when(customerIdentityLinkService.getRequiredCurrentCustomer(44L, "client@test.com"))
                .thenReturn(Customer.builder().id(9).email("client@test.com").build());
        when(vehicleService.getAllByCustomerId(9)).thenReturn(List.of(VehicleResponseDTO.builder().id(7).build()));

        List<VehicleResponseDTO> response = customerSelfService.getCurrentCustomerVehicles();

        assertThat(response).extracting(VehicleResponseDTO::getId).containsExactly(7);
    }

    @Test
    void updateCurrentCustomerShouldRejectEmailMutation() {
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(
                new AuthenticatedUser(44L, "client@test.com", Set.of("CUSTOMER"), "jti", Instant.parse("2026-05-20T10:00:00Z")),
                null,
                java.util.List.of()
        ));
        when(customerIdentityLinkService.getRequiredCurrentCustomer(44L, "client@test.com"))
                .thenReturn(Customer.builder().id(9).email("client@test.com").phoneNumber("+79990001122").build());
        CustomerUpdateDTO dto = CustomerUpdateDTO.builder().email("new@test.com").build();

        assertThatThrownBy(() -> customerSelfService.updateCurrentCustomer(dto))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Customer email change must go through auth flow");
    }
}
