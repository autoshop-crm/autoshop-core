package com.vladko.autoshopcore.customerauth.service;

import com.vladko.autoshopcore.client.entity.Customer;
import com.vladko.autoshopcore.client.exception.CustomerConflictException;
import com.vladko.autoshopcore.client.repository.CustomerRepository;
import com.vladko.autoshopcore.customerauth.dto.CustomerAuthTokensDTO;
import com.vladko.autoshopcore.customerauth.exception.CustomerAuthLinkageException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CustomerIdentityLinkServiceTest {

    @Mock
    private CustomerRepository customerRepository;

    @InjectMocks
    private CustomerIdentityLinkService customerIdentityLinkService;

    @Test
    void ensureLinkedCustomerCreatesMissingLocalProfileFromAuthData() {
        CustomerAuthTokensDTO auth = CustomerAuthTokensDTO.builder()
                .authUserId(44L)
                .email("client@test.com")
                .phoneNumber("+79990001122")
                .firstName("Ivan")
                .lastName("Petrov")
                .emailVerified(true)
                .build();

        when(customerRepository.findByAuthUserId(44L)).thenReturn(Optional.empty());
        when(customerRepository.findByEmail("client@test.com")).thenReturn(Optional.empty());
        when(customerRepository.findByPhoneNumber("+79990001122")).thenReturn(Optional.empty());
        when(customerRepository.existsByAuthUserId(44L)).thenReturn(false);
        when(customerRepository.existsByEmail("client@test.com")).thenReturn(false);
        when(customerRepository.existsByPhoneNumber("+79990001122")).thenReturn(false);
        when(customerRepository.save(any(Customer.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Customer customer = customerIdentityLinkService.ensureLinkedCustomer(auth);

        assertThat(customer.getAuthUserId()).isEqualTo(44L);
        assertThat(customer.getEmail()).isEqualTo("client@test.com");
        assertThat(customer.getPhoneNumber()).isEqualTo("+79990001122");
        assertThat(customer.getFirstName()).isEqualTo("Ivan");
        assertThat(customer.getLastName()).isEqualTo("Petrov");
        assertThat(customer.getEmailVerified()).isTrue();
    }

    @Test
    void ensureLinkedCustomerFallsBackToPhoneAndLinksExistingProfile() {
        CustomerAuthTokensDTO auth = CustomerAuthTokensDTO.builder()
                .authUserId(44L)
                .email("client@test.com")
                .phoneNumber("+79990001122")
                .firstName("Ivan")
                .lastName("Petrov")
                .emailVerified(true)
                .build();
        Customer existing = Customer.builder()
                .id(9)
                .email("legacy@test.com")
                .phoneNumber("+79990001122")
                .firstName("Legacy")
                .lastName("Customer")
                .build();

        when(customerRepository.findByAuthUserId(44L)).thenReturn(Optional.empty());
        when(customerRepository.findByEmail("client@test.com")).thenReturn(Optional.empty());
        when(customerRepository.findByPhoneNumber("+79990001122")).thenReturn(Optional.of(existing), Optional.of(existing));
        when(customerRepository.findByAuthUserId(44L)).thenReturn(Optional.empty(), Optional.empty());
        when(customerRepository.save(existing)).thenReturn(existing);

        Customer customer = customerIdentityLinkService.ensureLinkedCustomer(auth);

        assertThat(customer.getAuthUserId()).isEqualTo(44L);
        assertThat(customer.getEmail()).isEqualTo("client@test.com");
        assertThat(customer.getFirstName()).isEqualTo("Ivan");
    }

    @Test
    void ensureLinkedCustomerFailsWhenAnotherCustomerAlreadyUsesTargetEmail() {
        CustomerAuthTokensDTO auth = CustomerAuthTokensDTO.builder()
                .authUserId(44L)
                .email("client@test.com")
                .phoneNumber("+79990001122")
                .build();
        Customer existing = Customer.builder()
                .id(9)
                .email("legacy@test.com")
                .phoneNumber("+79990001122")
                .build();
        Customer emailOwner = Customer.builder()
                .id(11)
                .email("client@test.com")
                .phoneNumber("+79990003344")
                .build();

        when(customerRepository.findByAuthUserId(44L)).thenReturn(Optional.empty(), Optional.empty());
        when(customerRepository.findByEmail("client@test.com")).thenReturn(Optional.empty(), Optional.of(emailOwner));
        when(customerRepository.findByPhoneNumber("+79990001122")).thenReturn(Optional.of(existing), Optional.of(existing));

        assertThatThrownBy(() -> customerIdentityLinkService.ensureLinkedCustomer(auth))
                .isInstanceOf(CustomerConflictException.class)
                .hasMessage("Customer with email 'client@test.com' already exists");
    }

    @Test
    void ensureLinkedCustomerUsesFallbackNamesWhenAuthNamesAreMissing() {
        CustomerAuthTokensDTO auth = CustomerAuthTokensDTO.builder()
                .authUserId(44L)
                .email("ab@test.com")
                .phoneNumber("+79990001122")
                .emailVerified(false)
                .build();

        when(customerRepository.findByAuthUserId(44L)).thenReturn(Optional.empty());
        when(customerRepository.findByEmail("ab@test.com")).thenReturn(Optional.empty());
        when(customerRepository.findByPhoneNumber("+79990001122")).thenReturn(Optional.empty());
        when(customerRepository.existsByAuthUserId(44L)).thenReturn(false);
        when(customerRepository.existsByEmail("ab@test.com")).thenReturn(false);
        when(customerRepository.existsByPhoneNumber("+79990001122")).thenReturn(false);
        when(customerRepository.save(any(Customer.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Customer customer = customerIdentityLinkService.ensureLinkedCustomer(auth);

        assertThat(customer.getFirstName()).isEqualTo("ab");
        assertThat(customer.getLastName()).isEqualTo("Customer");
        assertThat(customer.getEmailVerified()).isFalse();
    }
}
