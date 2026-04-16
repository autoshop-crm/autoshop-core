package com.vladko.autoshopcore.client.service;

import com.vladko.autoshopcore.client.dto.CustomerCreateDTO;
import com.vladko.autoshopcore.client.dto.CustomerResponseDTO;
import com.vladko.autoshopcore.client.dto.CustomerUpdateDTO;
import com.vladko.autoshopcore.client.entity.Customer;
import com.vladko.autoshopcore.client.exception.CustomerConflictException;
import com.vladko.autoshopcore.client.exception.CustomerNotFoundException;
import com.vladko.autoshopcore.client.repository.CustomerRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;
import java.util.Locale;

@Service
@RequiredArgsConstructor
public class CustomerServiceImpl implements CustomerService {

    private final CustomerRepository customerRepository;

    @Override
    @Transactional
    public CustomerResponseDTO create(CustomerCreateDTO dto) {
        String normalizedEmail = normalizeEmail(dto.getEmail());
        String normalizedPhoneNumber = normalizePhoneNumber(dto.getPhoneNumber());

        validateEmailAvailability(normalizedEmail, null);
        validatePhoneAvailability(normalizedPhoneNumber, null);

        Customer customer = Customer.builder()
                .firstName(normalizeText(dto.getFirstName()))
                .lastName(normalizeText(dto.getLastName()))
                .email(normalizedEmail)
                .phoneNumber(normalizedPhoneNumber)
                .build();

        return mapToResponse(customerRepository.save(customer));
    }

    @Override
    @Transactional(readOnly = true)
    public CustomerResponseDTO getById(Integer id) {
        return mapToResponse(findCustomer(id));
    }

    @Override
    @Transactional
    public CustomerResponseDTO update(Integer id, CustomerUpdateDTO dto) {
        Customer customer = findCustomer(id);

        String normalizedFirstName = normalizeOptionalText(dto.getFirstName());
        String normalizedLastName = normalizeOptionalText(dto.getLastName());
        String normalizedEmail = normalizeOptionalEmail(dto.getEmail());
        String normalizedPhoneNumber = normalizeOptionalPhoneNumber(dto.getPhoneNumber());

        if (normalizedFirstName != null) {
            customer.setFirstName(normalizedFirstName);
        }

        if (normalizedLastName != null) {
            customer.setLastName(normalizedLastName);
        }

        if (normalizedEmail != null && !normalizedEmail.equals(customer.getEmail())) {
            validateEmailAvailability(normalizedEmail, customer.getId());
            customer.setEmail(normalizedEmail);
        }

        if (normalizedPhoneNumber != null && !normalizedPhoneNumber.equals(customer.getPhoneNumber())) {
            validatePhoneAvailability(normalizedPhoneNumber, customer.getId());
            customer.setPhoneNumber(normalizedPhoneNumber);
        }

        return mapToResponse(customerRepository.save(customer));
    }

    @Override
    @Transactional
    public void delete(Integer id) {
        customerRepository.delete(findCustomer(id));
    }

    @Override
    @Transactional(readOnly = true)
    public List<CustomerResponseDTO> search(String email, String phoneNumber, String firstName, String lastName) {
        String normalizedEmail = normalizeOptionalEmail(email);
        String normalizedPhoneNumber = normalizeOptionalPhoneNumber(phoneNumber);
        String normalizedFirstName = normalizeOptionalText(firstName);
        String normalizedLastName = normalizeOptionalText(lastName);

        return customerRepository.findAll()
                .stream()
                .filter(customer -> matchesEmail(customer, normalizedEmail))
                .filter(customer -> matchesPhoneNumber(customer, normalizedPhoneNumber))
                .filter(customer -> matchesFirstName(customer, normalizedFirstName))
                .filter(customer -> matchesLastName(customer, normalizedLastName))
                .sorted(Comparator.comparing(Customer::getId))
                .map(this::mapToResponse)
                .toList();
    }

    private Customer findCustomer(Integer id) {
        return customerRepository.findById(id)
                .orElseThrow(() -> new CustomerNotFoundException(id));
    }

    private void validateEmailAvailability(String email, Integer currentCustomerId) {
        customerRepository.findByEmail(email)
                .filter(customer -> !customer.getId().equals(currentCustomerId))
                .ifPresent(customer -> {
                    throw new CustomerConflictException("Customer with email '%s' already exists".formatted(email));
                });
    }

    private void validatePhoneAvailability(String phoneNumber, Integer currentCustomerId) {
        customerRepository.findByPhoneNumber(phoneNumber)
                .filter(customer -> !customer.getId().equals(currentCustomerId))
                .ifPresent(customer -> {
                    throw new CustomerConflictException(
                            "Customer with phone number '%s' already exists".formatted(phoneNumber)
                    );
                });
    }

    private CustomerResponseDTO mapToResponse(Customer customer) {
        return CustomerResponseDTO.builder()
                .id(customer.getId())
                .firstName(customer.getFirstName())
                .lastName(customer.getLastName())
                .email(customer.getEmail())
                .phoneNumber(customer.getPhoneNumber())
                .createdAt(customer.getCreatedAt())
                .updatedAt(customer.getUpdatedAt())
                .build();
    }

    private boolean matchesEmail(Customer customer, String email) {
        return email == null || customer.getEmail().equalsIgnoreCase(email);
    }

    private boolean matchesPhoneNumber(Customer customer, String phoneNumber) {
        return phoneNumber == null || customer.getPhoneNumber().equals(phoneNumber);
    }

    private boolean matchesFirstName(Customer customer, String firstName) {
        return firstName == null
                || customer.getFirstName().toLowerCase(Locale.ROOT).contains(firstName.toLowerCase(Locale.ROOT));
    }

    private boolean matchesLastName(Customer customer, String lastName) {
        return lastName == null
                || customer.getLastName().toLowerCase(Locale.ROOT).contains(lastName.toLowerCase(Locale.ROOT));
    }

    private String normalizeEmail(String email) {
        return normalizeText(email).toLowerCase(Locale.ROOT);
    }

    private String normalizeOptionalEmail(String email) {
        String normalizedEmail = normalizeOptionalText(email);
        return normalizedEmail == null ? null : normalizedEmail.toLowerCase(Locale.ROOT);
    }

    private String normalizePhoneNumber(String phoneNumber) {
        return normalizeText(phoneNumber);
    }

    private String normalizeOptionalPhoneNumber(String phoneNumber) {
        return normalizeOptionalText(phoneNumber);
    }

    private String normalizeText(String value) {
        String normalizedValue = normalizeOptionalText(value);
        if (normalizedValue == null) {
            throw new IllegalArgumentException("Value must not be blank");
        }
        return normalizedValue;
    }

    private String normalizeOptionalText(String value) {
        if (value == null) {
            return null;
        }

        String normalizedValue = value.trim();
        return normalizedValue.isEmpty() ? null : normalizedValue;
    }
}
