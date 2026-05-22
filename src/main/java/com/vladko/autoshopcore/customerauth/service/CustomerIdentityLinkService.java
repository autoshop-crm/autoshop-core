package com.vladko.autoshopcore.customerauth.service;

import com.vladko.autoshopcore.client.dto.CustomerCreateDTO;
import com.vladko.autoshopcore.client.entity.Customer;
import com.vladko.autoshopcore.client.exception.CustomerConflictException;
import com.vladko.autoshopcore.client.exception.CustomerNotFoundException;
import com.vladko.autoshopcore.client.repository.CustomerRepository;
import com.vladko.autoshopcore.customerauth.dto.CustomerAuthTokensDTO;
import com.vladko.autoshopcore.customerauth.exception.CustomerAuthLinkageException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Locale;

@Service
@RequiredArgsConstructor
public class CustomerIdentityLinkService {

    private final CustomerRepository customerRepository;

    @Transactional(readOnly = true)
    public Customer getRequiredCurrentCustomer(Long authUserId, String email) {
        return resolveCurrentCustomer(authUserId, email)
                .orElseThrow(() -> new CustomerNotFoundException("current authenticated customer"));
    }

    @Transactional(readOnly = true)
    public java.util.Optional<Customer> resolveCurrentCustomer(Long authUserId, String email) {
        if (authUserId != null) {
            var byAuthUserId = customerRepository.findByAuthUserId(authUserId);
            if (byAuthUserId.isPresent()) {
                return byAuthUserId;
            }
        }
        String normalizedEmail = normalizeEmail(email);
        if (normalizedEmail == null) {
            return java.util.Optional.empty();
        }
        return customerRepository.findByEmail(normalizedEmail);
    }

    @Transactional
    public Customer ensureCustomerForRegistration(CustomerRegisterPayload payload) {
        String normalizedEmail = normalizeRequiredEmail(payload.email());
        String normalizedPhone = normalizeRequiredPhone(payload.phoneNumber());
        customerRepository.findByEmail(normalizedEmail).ifPresent(existing -> {
            throw new CustomerConflictException("Customer with email '%s' already exists".formatted(normalizedEmail));
        });
        customerRepository.findByPhoneNumber(normalizedPhone).ifPresent(existing -> {
            throw new CustomerConflictException("Customer with phone number '%s' already exists".formatted(normalizedPhone));
        });
        if (payload.authUserId() != null && customerRepository.existsByAuthUserId(payload.authUserId())) {
            throw new CustomerAuthLinkageException("Auth user is already linked to another customer");
        }
        Customer customer = Customer.builder()
                .firstName(normalizeRequiredText(payload.firstName()))
                .lastName(normalizeRequiredText(payload.lastName()))
                .email(normalizedEmail)
                .phoneNumber(normalizedPhone)
                .authUserId(payload.authUserId())
                .emailVerified(payload.emailVerified())
                .build();
        return customerRepository.save(customer);
    }

    @Transactional
    public Customer ensureLinkedCustomer(CustomerAuthTokensDTO auth) {
        if (auth.getAuthUserId() != null) {
            var byAuthUserId = customerRepository.findByAuthUserId(auth.getAuthUserId());
            if (byAuthUserId.isPresent()) {
                return updateFromAuth(byAuthUserId.get(), auth);
            }
        }
        String normalizedEmail = normalizeRequiredEmail(auth.getEmail());
        Customer customer = customerRepository.findByEmail(normalizedEmail)
                .orElseThrow(() -> new CustomerAuthLinkageException(
                        "Authenticated customer '%s' is not linked to local profile".formatted(normalizedEmail)
                ));
        return updateFromAuth(customer, auth);
    }

    private Customer updateFromAuth(Customer customer, CustomerAuthTokensDTO auth) {
        if (customer.getAuthUserId() != null && auth.getAuthUserId() != null && !customer.getAuthUserId().equals(auth.getAuthUserId())) {
            throw new CustomerAuthLinkageException("Customer is already linked to another auth user");
        }
        customer.setAuthUserId(auth.getAuthUserId());
        customer.setEmail(normalizeRequiredEmail(auth.getEmail()));
        if (auth.getPhoneNumber() != null && !auth.getPhoneNumber().isBlank()) {
            customer.setPhoneNumber(normalizeRequiredPhone(auth.getPhoneNumber()));
        }
        if (auth.getFirstName() != null && !auth.getFirstName().isBlank()) {
            customer.setFirstName(normalizeRequiredText(auth.getFirstName()));
        }
        if (auth.getLastName() != null && !auth.getLastName().isBlank()) {
            customer.setLastName(normalizeRequiredText(auth.getLastName()));
        }
        customer.setEmailVerified(auth.isEmailVerified());
        return customerRepository.save(customer);
    }

    private String normalizeRequiredEmail(String email) {
        String normalized = normalizeEmail(email);
        if (normalized == null) {
            throw new IllegalArgumentException("Email must not be blank");
        }
        return normalized;
    }

    private String normalizeRequiredPhone(String phone) {
        String normalized = normalizeRequiredText(phone);
        return normalized;
    }

    private String normalizeRequiredText(String value) {
        if (value == null || value.trim().isEmpty()) {
            throw new IllegalArgumentException("Value must not be blank");
        }
        return value.trim();
    }

    private String normalizeEmail(String email) {
        if (email == null || email.trim().isEmpty()) {
            return null;
        }
        return email.trim().toLowerCase(Locale.ROOT);
    }

    public record CustomerRegisterPayload(Long authUserId,
                                          String email,
                                          String phoneNumber,
                                          String firstName,
                                          String lastName,
                                          boolean emailVerified) {}
}
