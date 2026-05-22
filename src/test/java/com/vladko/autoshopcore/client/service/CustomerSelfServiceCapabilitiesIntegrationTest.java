package com.vladko.autoshopcore.client.service;

import com.vladko.autoshopcore.PostgresTestcontainersConfiguration;
import com.vladko.autoshopcore.client.dto.CustomerVehicleCreateDTO;
import com.vladko.autoshopcore.client.dto.CustomerVehicleUpdateDTO;
import com.vladko.autoshopcore.client.entity.Customer;
import com.vladko.autoshopcore.client.repository.CustomerRepository;
import com.vladko.autoshopcore.security.AuthenticatedUser;
import com.vladko.autoshopcore.vehicle.dto.VehicleResponseDTO;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import java.time.Instant;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest(properties = {
        "spring.data.redis.repositories.enabled=false"
})
@Import(PostgresTestcontainersConfiguration.class)
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class CustomerSelfServiceCapabilitiesIntegrationTest {

    @Autowired
    private CustomerVehicleSelfService customerVehicleSelfService;

    @Autowired
    private CustomerRepository customerRepository;

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void customerCanCreateUpdateAndDeleteOwnVehicle() {
        Customer customer = authenticateNewCustomer("vehicle-owner@example.com", "+79990000411", 511L);

        VehicleResponseDTO created = customerVehicleSelfService.createVehicle(CustomerVehicleCreateDTO.builder()
                .brand("BMW")
                .model("X5")
                .vin("WBAAA111111111111")
                .licensePlate("A111AA77")
                .build());

        assertThat(created.getCustomerId()).isEqualTo(customer.getId());
        assertThat(customerVehicleSelfService.getCurrentCustomerVehicles()).extracting(VehicleResponseDTO::getId).contains(created.getId());

        VehicleResponseDTO updated = customerVehicleSelfService.updateVehicle(created.getId(), CustomerVehicleUpdateDTO.builder()
                .model("X5 LCI")
                .licensePlate("B222BB77")
                .build());

        assertThat(updated.getModel()).isEqualTo("X5 LCI");
        assertThat(updated.getLicensePlate()).isEqualTo("B222BB77");

        customerVehicleSelfService.deleteVehicle(created.getId());
        assertThat(customerVehicleSelfService.getCurrentCustomerVehicles()).extracting(VehicleResponseDTO::getId).doesNotContain(created.getId());
    }

    @Test
    void customerCannotAccessForeignVehicle() {
        authenticateNewCustomer("vehicle-owner-1@example.com", "+79990000412", 512L);
        VehicleResponseDTO foreignVehicle = customerVehicleSelfService.createVehicle(CustomerVehicleCreateDTO.builder()
                .brand("Audi")
                .model("Q7")
                .vin("WAUZZZ11111111111")
                .licensePlate("C333CC77")
                .build());

        authenticateNewCustomer("vehicle-owner-2@example.com", "+79990000413", 513L);

        assertThatThrownBy(() -> customerVehicleSelfService.getCurrentCustomerVehicle(foreignVehicle.getId()))
                .isInstanceOf(AccessDeniedException.class);
    }

    private Customer authenticateNewCustomer(String email, String phoneNumber, Long authUserId) {
        Customer customer = customerRepository.save(Customer.builder()
                .firstName("Test")
                .lastName("Customer")
                .email(email)
                .phoneNumber(phoneNumber)
                .authUserId(authUserId)
                .emailVerified(true)
                .build());

        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(
                new AuthenticatedUser(authUserId, email, Set.of("CUSTOMER"), "jti", Instant.parse("2026-05-20T10:00:00Z")),
                null,
                List.of(new SimpleGrantedAuthority("ROLE_CUSTOMER"))
        ));

        return customer;
    }
}
