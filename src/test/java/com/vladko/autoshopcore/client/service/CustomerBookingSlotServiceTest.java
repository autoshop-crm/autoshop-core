package com.vladko.autoshopcore.client.service;

import com.vladko.autoshopcore.client.entity.Customer;
import com.vladko.autoshopcore.customerauth.service.CustomerIdentityLinkService;
import com.vladko.autoshopcore.employee.dto.EmployeeAvailabilityResponseDTO;
import com.vladko.autoshopcore.employee.service.EmployeeService;
import com.vladko.autoshopcore.security.AuthenticatedUser;
import com.vladko.autoshopcore.servicecatalog.dto.ServiceCatalogItemResponseDTO;
import com.vladko.autoshopcore.servicecatalog.service.ServiceCatalogService;
import com.vladko.autoshopcore.vehicle.dto.VehicleResponseDTO;
import com.vladko.autoshopcore.vehicle.service.VehicleService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CustomerBookingSlotServiceTest {

    @Mock private CustomerIdentityLinkService customerIdentityLinkService;
    @Mock private VehicleService vehicleService;
    @Mock private EmployeeService employeeService;
    @Mock private ServiceCatalogService serviceCatalogService;

    @InjectMocks private CustomerBookingSlotServiceImpl customerBookingSlotService;

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void lookupSlotsShouldReturnAvailabilityForComputedDuration() {
        authenticateCustomer();
        when(customerIdentityLinkService.getRequiredCurrentCustomer(44L, "client@test.com"))
                .thenReturn(Customer.builder().id(9).email("client@test.com").build());
        when(vehicleService.getById(15)).thenReturn(VehicleResponseDTO.builder().id(15).customerId(9).build());
        when(serviceCatalogService.getServices(true, null)).thenReturn(List.of(
                ServiceCatalogItemResponseDTO.builder().id(1).name("A").basePrice(BigDecimal.ONE).defaultDurationMinutes(60).build(),
                ServiceCatalogItemResponseDTO.builder().id(2).name("B").basePrice(BigDecimal.ONE).defaultDurationMinutes(30).build()
        ));
        when(employeeService.searchAvailability(any(), any(), any(), eq(90), eq(20))).thenReturn(List.of(
                EmployeeAvailabilityResponseDTO.builder().id(1).available(true).build(),
                EmployeeAvailabilityResponseDTO.builder().id(2).available(false).build()
        ));

        var slots = customerBookingSlotService.lookupSlots(15, List.of(1, 2), LocalDate.of(2026, 5, 20), 1, null);

        assertThat(slots).isNotEmpty();
        assertThat(slots.get(0).getSlotMinutes()).isEqualTo(90);
        assertThat(slots.get(0).isAvailable()).isTrue();
        assertThat(slots.get(0).getAvailableEmployeeCount()).isEqualTo(1);
    }

    private void authenticateCustomer() {
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(
                new AuthenticatedUser(44L, "client@test.com", Set.of("CUSTOMER"), "jti", java.time.Instant.parse("2026-05-20T10:00:00Z")),
                null,
                List.of(new SimpleGrantedAuthority("ROLE_CUSTOMER"))
        ));
    }
}
