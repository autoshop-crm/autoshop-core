package com.vladko.autoshopcore.order.repository;

import com.vladko.autoshopcore.PostgresTestcontainersConfiguration;
import com.vladko.autoshopcore.entities.Employee;
import com.vladko.autoshopcore.entities.EmployeeType;
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
class EmployeeRepositoryTest {

    @Autowired
    private EmployeeRepository employeeRepository;

    @Test
    void findAllByOrderByIdAscShouldReturnEmployeesOrderedById() {
        employeeRepository.save(Employee.builder()
                .firstName("Second")
                .lastName("User")
                .email("second@example.com")
                .function(EmployeeType.MANAGER)
                .build());
        employeeRepository.save(Employee.builder()
                .firstName("First")
                .lastName("User")
                .email("first@example.com")
                .function(EmployeeType.ADMIN)
                .build());

        List<Employee> employees = employeeRepository.findAllByOrderByIdAsc();

        assertThat(employees)
                .extracting(Employee::getId)
                .isSorted();
    }

    @Test
    void saveShouldPopulateCreatedAt() {
        Employee saved = employeeRepository.save(Employee.builder()
                .firstName("Created")
                .lastName("At")
                .email("created-at@example.com")
                .function(EmployeeType.ADMIN)
                .build());

        assertThat(saved.getCreatedAt()).isNotNull();
    }

    @Test
    void searchByEmailPrefixShouldReturnMatchingEmployees() {
        employeeRepository.save(Employee.builder()
                .firstName("Anna")
                .lastName("Admin")
                .email("anna@example.com")
                .function(EmployeeType.ADMIN)
                .build());
        employeeRepository.save(Employee.builder()
                .firstName("Anatoly")
                .lastName("Manager")
                .email("anatoly@example.com")
                .function(EmployeeType.MANAGER)
                .build());
        employeeRepository.save(Employee.builder()
                .firstName("Petr")
                .lastName("Mechanic")
                .email("petr@example.com")
                .function(EmployeeType.MECHANIC)
                .build());

        List<Employee> employees = employeeRepository.searchByEmailPrefix("ana", 10);

        assertThat(employees)
                .extracting(Employee::getEmail)
                .containsExactly("anatoly@example.com");
    }
}
