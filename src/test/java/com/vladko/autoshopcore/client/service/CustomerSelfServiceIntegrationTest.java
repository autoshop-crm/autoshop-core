package com.vladko.autoshopcore.client.service;

import com.vladko.autoshopcore.PostgresTestcontainersConfiguration;
import com.vladko.autoshopcore.client.dto.CustomerLoyaltyOverviewDTO;
import com.vladko.autoshopcore.client.dto.CustomerSelfServiceDashboardDTO;
import com.vladko.autoshopcore.client.entity.Customer;
import com.vladko.autoshopcore.client.repository.CustomerRepository;
import com.vladko.autoshopcore.loyalty.repository.LoyaltyAccountRepository;
import com.vladko.autoshopcore.security.AuthenticatedUser;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.time.Instant;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(properties = {
        "spring.data.redis.repositories.enabled=false"
})
@Import(PostgresTestcontainersConfiguration.class)
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class CustomerSelfServiceIntegrationTest {

    @Autowired
    private CustomerSelfService customerSelfService;

    @Autowired
    private CustomerRepository customerRepository;

    @Autowired
    private LoyaltyAccountRepository loyaltyAccountRepository;

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void getCurrentCustomerLoyaltyShouldCreateMissingAccount() {
        Customer customer = authenticateNewCustomer("customer-loyalty@example.com", "+79990000401", 501L);

        assertThat(loyaltyAccountRepository.findByCustomerId(customer.getId())).isEmpty();

        CustomerLoyaltyOverviewDTO response = customerSelfService.getCurrentCustomerLoyalty();

        assertThat(response.getAccount()).isNotNull();
        assertThat(response.getAccount().getCustomerId()).isEqualTo(customer.getId());
        assertThat(loyaltyAccountRepository.findByCustomerId(customer.getId())).isPresent();
    }

    @Test
    void getCurrentCustomerDashboardShouldCreateMissingAccountAndReturnPayload() {
        Customer customer = authenticateNewCustomer("customer-dashboard@example.com", "+79990000402", 502L);

        assertThat(loyaltyAccountRepository.findByCustomerId(customer.getId())).isEmpty();

        CustomerSelfServiceDashboardDTO response = customerSelfService.getCurrentCustomerDashboard();

        assertThat(response.getCustomer()).isNotNull();
        assertThat(response.getCustomer().getId()).isEqualTo(customer.getId());
        assertThat(response.getRecentOrders()).isEmpty();
        assertThat(response.getPendingApprovals()).isEmpty();
        assertThat(response.getVehicles()).isEmpty();
        assertThat(response.getLoyalty()).isNotNull();
        assertThat(response.getLoyalty().getAccount()).isNotNull();
        assertThat(loyaltyAccountRepository.findByCustomerId(customer.getId())).isPresent();
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
                java.util.List.of()
        ));

        return customer;
    }
}
