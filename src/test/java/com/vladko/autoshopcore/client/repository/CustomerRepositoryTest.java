package com.vladko.autoshopcore.client.repository;

import com.vladko.autoshopcore.PostgresTestcontainersConfiguration;
import com.vladko.autoshopcore.client.entity.Customer;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@Import(PostgresTestcontainersConfiguration.class)
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class CustomerRepositoryTest {

    @Autowired
    private CustomerRepository customerRepository;

    @Test
    void existsByEmailShouldReturnTrueForPersistedCustomer() {
        customerRepository.save(Customer.builder()
                .firstName("Maria")
                .lastName("Ivanova")
                .email("maria@example.com")
                .phoneNumber("+79991112233")
                .build());

        assertThat(customerRepository.existsByEmail("maria@example.com")).isTrue();
    }

    @Test
    void findByFirstNameContainingIgnoreCaseShouldReturnMatchingCustomers() {
        customerRepository.save(Customer.builder()
                .firstName("Ivan")
                .lastName("Petrov")
                .email("ivan.petrov@example.com")
                .phoneNumber("+79990000001")
                .build());
        customerRepository.save(Customer.builder()
                .firstName("Ivan")
                .lastName("Sidorov")
                .email("ivan.sidorov@example.com")
                .phoneNumber("+79990000002")
                .build());

        List<Customer> customers = customerRepository.findByFirstNameContainingIgnoreCase("iva");

        assertThat(customers)
                .hasSize(2)
                .extracting(Customer::getEmail)
                .containsExactlyInAnyOrder("ivan.petrov@example.com", "ivan.sidorov@example.com");
    }
}
