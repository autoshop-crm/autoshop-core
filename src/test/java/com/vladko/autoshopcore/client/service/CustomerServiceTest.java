package com.vladko.autoshopcore.client.service;

import com.vladko.autoshopcore.client.dto.CustomerCreateDTO;
import com.vladko.autoshopcore.client.dto.CustomerResponseDTO;
import com.vladko.autoshopcore.client.dto.CustomerUpdateDTO;
import com.vladko.autoshopcore.client.entity.Customer;
import com.vladko.autoshopcore.client.exception.CustomerConflictException;
import com.vladko.autoshopcore.client.exception.CustomerNotFoundException;
import com.vladko.autoshopcore.client.repository.CustomerRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CustomerServiceTest {

    @Mock
    private CustomerRepository customerRepository;

    @InjectMocks
    private CustomerServiceImpl customerService;

    @Test
    void createShouldPersistNormalizedCustomer() {
        CustomerCreateDTO dto = CustomerCreateDTO.builder()
                .firstName("  Vladislav ")
                .lastName(" Kovrigin ")
                .email("  VLAD@example.com ")
                .phoneNumber(" +79991234567 ")
                .build();

        Customer savedCustomer = Customer.builder()
                .id(1)
                .firstName("Vladislav")
                .lastName("Kovrigin")
                .email("vlad@example.com")
                .phoneNumber("+79991234567")
                .createdAt(Instant.parse("2026-04-12T10:15:30Z"))
                .updatedAt(Instant.parse("2026-04-12T10:15:30Z"))
                .build();

        when(customerRepository.findByEmail("vlad@example.com")).thenReturn(Optional.empty());
        when(customerRepository.findByPhoneNumber("+79991234567")).thenReturn(Optional.empty());
        when(customerRepository.save(any(Customer.class))).thenReturn(savedCustomer);

        CustomerResponseDTO response = customerService.create(dto);

        ArgumentCaptor<Customer> customerCaptor = ArgumentCaptor.forClass(Customer.class);
        verify(customerRepository).save(customerCaptor.capture());

        Customer customerToSave = customerCaptor.getValue();
        assertThat(customerToSave.getFirstName()).isEqualTo("Vladislav");
        assertThat(customerToSave.getLastName()).isEqualTo("Kovrigin");
        assertThat(customerToSave.getEmail()).isEqualTo("vlad@example.com");
        assertThat(customerToSave.getPhoneNumber()).isEqualTo("+79991234567");

        assertThat(response.getId()).isEqualTo(1);
        assertThat(response.getEmail()).isEqualTo("vlad@example.com");
        assertThat(response.getPhoneNumber()).isEqualTo("+79991234567");
    }

    @Test
    void createShouldThrowConflictWhenEmailAlreadyExists() {
        CustomerCreateDTO dto = CustomerCreateDTO.builder()
                .firstName("Ivan")
                .lastName("Petrov")
                .email("ivan@example.com")
                .phoneNumber("+79990001122")
                .build();

        when(customerRepository.findByEmail("ivan@example.com"))
                .thenReturn(Optional.of(Customer.builder().id(10).email("ivan@example.com").build()));

        assertThatThrownBy(() -> customerService.create(dto))
                .isInstanceOf(CustomerConflictException.class)
                .hasMessage("Customer with email 'ivan@example.com' already exists");

        verify(customerRepository, never()).save(any(Customer.class));
    }

    @Test
    void updateShouldThrowNotFoundWhenCustomerMissing() {
        when(customerRepository.findById(404)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> customerService.update(404, new CustomerUpdateDTO()))
                .isInstanceOf(CustomerNotFoundException.class)
                .hasMessage("Customer with id '404' was not found");
    }

    @Test
    void updateShouldChangeOnlyProvidedFields() {
        Customer existingCustomer = Customer.builder()
                .id(7)
                .firstName("Anna")
                .lastName("Sidorova")
                .email("anna@example.com")
                .phoneNumber("+79995554433")
                .createdAt(Instant.parse("2026-04-12T10:15:30Z"))
                .updatedAt(Instant.parse("2026-04-12T10:15:30Z"))
                .build();

        Customer updatedCustomer = Customer.builder()
                .id(7)
                .firstName("Anna")
                .lastName("Smirnova")
                .email("anna@example.com")
                .phoneNumber("+79995554433")
                .createdAt(existingCustomer.getCreatedAt())
                .updatedAt(Instant.parse("2026-04-12T11:15:30Z"))
                .build();

        CustomerUpdateDTO dto = CustomerUpdateDTO.builder()
                .lastName("  Smirnova ")
                .build();

        when(customerRepository.findById(7)).thenReturn(Optional.of(existingCustomer));
        when(customerRepository.save(existingCustomer)).thenReturn(updatedCustomer);

        CustomerResponseDTO response = customerService.update(7, dto);

        assertThat(existingCustomer.getFirstName()).isEqualTo("Anna");
        assertThat(existingCustomer.getLastName()).isEqualTo("Smirnova");
        assertThat(existingCustomer.getEmail()).isEqualTo("anna@example.com");
        assertThat(response.getLastName()).isEqualTo("Smirnova");
    }
}
