package com.vladko.autoshopcore.client.service;

import com.vladko.autoshopcore.client.dto.CustomerBookingCreateDTO;
import com.vladko.autoshopcore.client.dto.CustomerBookingServiceCatalogItemDTO;
import com.vladko.autoshopcore.client.dto.CustomerBookingUpdateDTO;
import com.vladko.autoshopcore.client.entity.Customer;
import com.vladko.autoshopcore.customerauth.service.CustomerIdentityLinkService;
import com.vladko.autoshopcore.order.dto.OrderCreateDTO;
import com.vladko.autoshopcore.order.dto.OrderResponseDTO;
import com.vladko.autoshopcore.order.dto.OrderStatusUpdateDTO;
import com.vladko.autoshopcore.order.entity.OrderStatus;
import com.vladko.autoshopcore.order.service.OrderService;
import com.vladko.autoshopcore.security.AuthenticatedUser;
import com.vladko.autoshopcore.servicecatalog.dto.ServiceCatalogItemResponseDTO;
import com.vladko.autoshopcore.servicecatalog.service.ServiceCatalogService;
import com.vladko.autoshopcore.vehicle.dto.VehicleResponseDTO;
import com.vladko.autoshopcore.vehicle.service.VehicleService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CustomerBookingSelfServiceTest {

    @Mock private CustomerIdentityLinkService customerIdentityLinkService;
    @Mock private VehicleService vehicleService;
    @Mock private OrderService orderService;
    @Mock private ServiceCatalogService serviceCatalogService;

    @InjectMocks private CustomerBookingSelfServiceImpl customerBookingSelfService;

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void getAvailableServicesShouldMapActiveCatalogItems() {
        when(serviceCatalogService.getServices(true, null)).thenReturn(List.of(
                ServiceCatalogItemResponseDTO.builder()
                        .id(7)
                        .name("Brake inspection")
                        .description("desc")
                        .basePrice(new BigDecimal("100.00"))
                        .categoryId(2)
                        .categoryName("Diagnostics")
                        .defaultDurationMinutes(90)
                        .build()
        ));

        List<CustomerBookingServiceCatalogItemDTO> response = customerBookingSelfService.getAvailableServices();

        assertThat(response).hasSize(1);
        assertThat(response.get(0).getId()).isEqualTo(7);
        assertThat(response.get(0).getDefaultDurationMinutes()).isEqualTo(90);
    }

    @Test
    void createBookingShouldBindAuthenticatedCustomerAndVehicle() {
        authenticateCustomer();
        when(customerIdentityLinkService.getRequiredCurrentCustomer(44L, "client@test.com"))
                .thenReturn(Customer.builder().id(9).email("client@test.com").build());
        when(vehicleService.getById(15)).thenReturn(VehicleResponseDTO.builder().id(15).customerId(9).build());
        when(orderService.createForCustomer(org.mockito.ArgumentMatchers.any(OrderCreateDTO.class)))
                .thenReturn(OrderResponseDTO.builder().id(100).customerId(9).vehicleId(15).status(OrderStatus.WAITING_FOR_VISIT).build());

        OrderResponseDTO response = customerBookingSelfService.createBooking(CustomerBookingCreateDTO.builder()
                .vehicleId(15)
                .plannedVisitAt(Instant.parse("2026-05-21T09:00:00Z"))
                .plannedSlotMinutes(90)
                .problem("Brake noise")
                .selectedServiceIds(List.of(1, 2))
                .intakeNotes("Need morning appointment")
                .build());

        ArgumentCaptor<OrderCreateDTO> captor = ArgumentCaptor.forClass(OrderCreateDTO.class);
        verify(orderService).createForCustomer(captor.capture());
        assertThat(captor.getValue().getCustomerId()).isEqualTo(9);
        assertThat(captor.getValue().getVehicleId()).isEqualTo(15);
        assertThat(captor.getValue().getSelectedServiceIds()).containsExactly(1, 2);
        assertThat(response.getStatus()).isEqualTo(OrderStatus.WAITING_FOR_VISIT);
    }

    @Test
    void cancelBookingShouldUseCustomerCancellationStatus() {
        authenticateCustomer();
        when(customerIdentityLinkService.getRequiredCurrentCustomer(44L, "client@test.com"))
                .thenReturn(Customer.builder().id(9).email("client@test.com").build());
        when(orderService.updateStatus(org.mockito.ArgumentMatchers.eq(100), org.mockito.ArgumentMatchers.any(OrderStatusUpdateDTO.class)))
                .thenReturn(OrderResponseDTO.builder().id(100).status(OrderStatus.CANCELLED_BY_CUSTOMER).build());

        OrderResponseDTO response = customerBookingSelfService.cancelBooking(100);

        ArgumentCaptor<OrderStatusUpdateDTO> captor = ArgumentCaptor.forClass(OrderStatusUpdateDTO.class);
        verify(orderService).updateStatus(org.mockito.ArgumentMatchers.eq(100), captor.capture());
        assertThat(captor.getValue().getStatus()).isEqualTo(OrderStatus.CANCELLED_BY_CUSTOMER);
        assertThat(response.getStatus()).isEqualTo(OrderStatus.CANCELLED_BY_CUSTOMER);
    }

    @Test
    void updateBookingShouldDelegateToCustomerOrderUpdate() {
        authenticateCustomer();
        when(customerIdentityLinkService.getRequiredCurrentCustomer(44L, "client@test.com"))
                .thenReturn(Customer.builder().id(9).email("client@test.com").build());
        when(orderService.updateForCustomer(org.mockito.ArgumentMatchers.eq(100), org.mockito.ArgumentMatchers.any()))
                .thenReturn(OrderResponseDTO.builder().id(100).problem("Updated").build());

        OrderResponseDTO response = customerBookingSelfService.updateBooking(100, CustomerBookingUpdateDTO.builder().problem("Updated").build());

        assertThat(response.getProblem()).isEqualTo("Updated");
        verify(orderService).updateForCustomer(org.mockito.ArgumentMatchers.eq(100), org.mockito.ArgumentMatchers.any());
    }

    @Test
    void getCurrentCustomerOrderShouldDelegateToOrderService() {
        authenticateCustomer();
        when(customerIdentityLinkService.getRequiredCurrentCustomer(44L, "client@test.com"))
                .thenReturn(Customer.builder().id(9).email("client@test.com").build());
        when(orderService.getById(100)).thenReturn(OrderResponseDTO.builder().id(100).build());

        OrderResponseDTO response = customerBookingSelfService.getCurrentCustomerOrder(100);

        assertThat(response.getId()).isEqualTo(100);
        verify(orderService).getById(100);
    }

    private void authenticateCustomer() {
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(
                new AuthenticatedUser(44L, "client@test.com", Set.of("CUSTOMER"), "jti", Instant.parse("2026-05-20T10:00:00Z")),
                null,
                List.of(new SimpleGrantedAuthority("ROLE_CUSTOMER"))
        ));
    }
}
