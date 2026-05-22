package com.vladko.autoshopcore.customerauth.service;

import com.vladko.autoshopcore.client.dto.CustomerResponseDTO;
import com.vladko.autoshopcore.client.entity.Customer;
import com.vladko.autoshopcore.client.service.CustomerSelfService;
import com.vladko.autoshopcore.customerauth.dto.*;
import com.vladko.autoshopcore.security.AuthenticatedUser;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.time.Instant;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CustomerAuthServiceTest {

    @Mock private CustomerAuthGateway customerAuthGateway;
    @Mock private CustomerIdentityLinkService customerIdentityLinkService;
    @Mock private CustomerSelfService customerSelfService;

    @InjectMocks private CustomerAuthServiceImpl customerAuthService;

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void loginShouldReturnLinkedCustomerAndTokens() {
        CustomerLoginRequestDTO request = new CustomerLoginRequestDTO();
        request.setEmail("client@test.com");
        request.setPassword("secret123");
        CustomerAuthTokensDTO tokens = CustomerAuthTokensDTO.builder()
                .authUserId(44L)
                .email("client@test.com")
                .roles(Set.of("CUSTOMER"))
                .accessToken("access")
                .refreshToken("refresh")
                .expiresAt(Instant.parse("2026-05-20T10:00:00Z"))
                .emailVerified(true)
                .build();
        when(customerAuthGateway.login(request)).thenReturn(tokens);
        when(customerIdentityLinkService.ensureLinkedCustomer(tokens)).thenReturn(Customer.builder().id(9).email("client@test.com").phoneNumber("+79990001122").build());

        CustomerAuthResponseDTO response = customerAuthService.login(request);

        assertThat(response.getCustomerId()).isEqualTo(9);
        assertThat(response.getAuthUserId()).isEqualTo(44L);
        assertThat(response.getAccessToken()).isEqualTo("access");
        assertThat(response.isEmailVerified()).isTrue();
    }

    @Test
    void meShouldReturnCurrentCustomerView() {
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(
                new AuthenticatedUser(44L, "client@test.com", Set.of("CUSTOMER"), "jti", Instant.parse("2026-05-20T10:00:00Z")),
                null,
                java.util.List.of()
        ));
        when(customerSelfService.getCurrentCustomer()).thenReturn(CustomerResponseDTO.builder().id(9).email("client@test.com").build());

        CustomerAuthMeResponseDTO response = customerAuthService.me();

        assertThat(response.isAuthenticated()).isTrue();
        assertThat(response.getAuthUserId()).isEqualTo(44L);
        assertThat(response.getCustomer().getId()).isEqualTo(9);
    }
}
