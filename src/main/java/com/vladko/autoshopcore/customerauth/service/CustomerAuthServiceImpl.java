package com.vladko.autoshopcore.customerauth.service;

import com.vladko.autoshopcore.client.dto.CustomerResponseDTO;
import com.vladko.autoshopcore.client.entity.Customer;
import com.vladko.autoshopcore.client.service.CustomerSelfService;
import com.vladko.autoshopcore.customerauth.dto.*;
import com.vladko.autoshopcore.security.AuthenticatedUser;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Locale;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class CustomerAuthServiceImpl implements CustomerAuthService {

    private final CustomerAuthGateway customerAuthGateway;
    private final CustomerIdentityLinkService customerIdentityLinkService;
    private final CustomerSelfService customerSelfService;

    @Override
    @Transactional
    public CustomerAuthResponseDTO register(CustomerRegisterRequestDTO request) {
        CustomerAuthTokensDTO auth = customerAuthGateway.register(request);
        Customer customer = customerIdentityLinkService.ensureCustomerForRegistration(
                new CustomerIdentityLinkService.CustomerRegisterPayload(
                        auth.getAuthUserId(),
                        auth.getEmail(),
                        auth.getPhoneNumber() != null ? auth.getPhoneNumber() : request.getPhoneNumber(),
                        auth.getFirstName() != null ? auth.getFirstName() : request.getFirstName(),
                        auth.getLastName() != null ? auth.getLastName() : request.getLastName(),
                        auth.isEmailVerified()
                )
        );
        return toResponse(customer, auth);
    }

    @Override
    @Transactional
    public CustomerAuthResponseDTO login(CustomerLoginRequestDTO request) {
        CustomerAuthTokensDTO auth = customerAuthGateway.login(request);
        Customer customer = customerIdentityLinkService.ensureLinkedCustomer(auth);
        return toResponse(customer, auth);
    }

    @Override
    @Transactional
    public CustomerAuthResponseDTO refresh(CustomerRefreshRequestDTO request) {
        CustomerAuthTokensDTO auth = customerAuthGateway.refresh(request);
        Customer customer = customerIdentityLinkService.ensureLinkedCustomer(auth);
        return toResponse(customer, auth);
    }

    @Override
    public void logout(CustomerLogoutRequestDTO request) {
        customerAuthGateway.logout(request);
    }

    @Override
    public void forgotPassword(CustomerForgotPasswordRequestDTO request) {
        customerAuthGateway.forgotPassword(request);
    }

    @Override
    public void resetPassword(CustomerResetPasswordRequestDTO request) {
        customerAuthGateway.resetPassword(request);
    }

    @Override
    @Transactional
    public CustomerAuthResponseDTO verifyEmail(CustomerVerifyEmailRequestDTO request) {
        CustomerAuthTokensDTO auth = customerAuthGateway.verifyEmail(request);
        Customer customer = customerIdentityLinkService.ensureLinkedCustomer(auth);
        return toResponse(customer, auth);
    }

    @Override
    @Transactional(readOnly = true)
    public CustomerAuthMeResponseDTO me() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof AuthenticatedUser authenticatedUser)) {
            return CustomerAuthMeResponseDTO.builder().authenticated(false).profileCompleted(false).build();
        }
        CustomerResponseDTO currentCustomer = customerSelfService.getCurrentCustomer();
        return CustomerAuthMeResponseDTO.builder()
                .authenticated(true)
                .authUserId(authenticatedUser.userId())
                .email(authenticatedUser.email())
                .roles(normalizeRoles(authenticatedUser.roles()))
                .emailVerified(Boolean.TRUE.equals(currentCustomer.getEmailVerified()))
                .customer(currentCustomer)
                .profileCompleted(true)
                .build();
    }

    private Set<String> normalizeRoles(Set<String> roles) {
        return roles == null ? Set.of() : roles.stream().map(role -> role.toUpperCase(Locale.ROOT)).collect(java.util.stream.Collectors.toSet());
    }

    private CustomerAuthResponseDTO toResponse(Customer customer, CustomerAuthTokensDTO auth) {
        return CustomerAuthResponseDTO.builder()
                .customerId(customer.getId())
                .authUserId(auth.getAuthUserId())
                .email(customer.getEmail())
                .phoneNumber(customer.getPhoneNumber())
                .roles(normalizeRoles(auth.getRoles()))
                .accessToken(auth.getAccessToken())
                .refreshToken(auth.getRefreshToken())
                .expiresAt(auth.getExpiresAt())
                .emailVerified(auth.isEmailVerified())
                .profileCompleted(true)
                .requiresEmailVerification(!auth.isEmailVerified())
                .build();
    }
}
