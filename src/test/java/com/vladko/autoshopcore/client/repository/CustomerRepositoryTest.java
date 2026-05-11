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

    @Test
    void searchByEmailPrefixShouldReturnLimitedRelevantCustomers() {
        customerRepository.save(Customer.builder()
                .firstName("Ivan")
                .lastName("Petrov")
                .email("ivan@example.com")
                .phoneNumber("+79990000001")
                .build());
        customerRepository.save(Customer.builder()
                .firstName("Ivanna")
                .lastName("Sidorova")
                .email("ivanna@example.com")
                .phoneNumber("+79990000002")
                .build());
        customerRepository.save(Customer.builder()
                .firstName("Petr")
                .lastName("Ivanov")
                .email("petr@example.com")
                .phoneNumber("+79990000003")
                .build());

        List<Customer> customers = customerRepository.searchByEmailPrefix("ivan", 10);

        assertThat(customers)
                .extracting(Customer::getEmail)
                .containsExactly("ivanna@example.com", "ivan@example.com");
    }

    @Test
    void searchByPhoneDigitsPrefixShouldIgnoreFormatting() {
        customerRepository.save(Customer.builder()
                .firstName("Maria")
                .lastName("Ivanova")
                .email("maria@example.com")
                .phoneNumber("+79991112233")
                .build());
        customerRepository.save(Customer.builder()
                .firstName("Anna")
                .lastName("Petrova")
                .email("anna@example.com")
                .phoneNumber("89994445566")
                .build());

        List<Customer> customers = customerRepository.searchByPhoneDigitsPrefix("7999", 10);

        assertThat(customers)
                .hasSize(1)
                .extracting(Customer::getEmail)
                .containsExactly("maria@example.com");
    }
}
