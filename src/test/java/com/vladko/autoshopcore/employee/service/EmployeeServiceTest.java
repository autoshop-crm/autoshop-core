package com.vladko.autoshopcore.employee.service;

import com.vladko.autoshopcore.employee.dto.EmployeeResponseDTO;
import com.vladko.autoshopcore.entities.Employee;
import com.vladko.autoshopcore.entities.EmployeeType;
import com.vladko.autoshopcore.order.repository.EmployeeRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EmployeeServiceTest {

    @Mock
    private EmployeeRepository employeeRepository;

    private EmployeeService employeeService;

    @BeforeEach
    void setUp() {
        employeeService = new EmployeeServiceImpl(employeeRepository);
    }

    @Test
    void getAllShouldReturnEmployeesOrderedFromRepository() {
        Employee employee = Employee.builder()
                .id(1)
                .firstName("Ivan")
                .lastName("Petrov")
                .email("ivan@example.com")
                .function(EmployeeType.MANAGER)
                .createdAt(Instant.parse("2026-05-11T10:15:30Z"))
                .build();

        when(employeeRepository.findAllByOrderByIdAsc()).thenReturn(List.of(employee));

        List<EmployeeResponseDTO> response = employeeService.getAll();

        assertThat(response)
                .hasSize(1)
                .extracting(EmployeeResponseDTO::getEmail)
                .containsExactly("ivan@example.com");
    }

    @Test
    void searchShouldReturnEmptyListForPlainTextQuery() {
        assertThat(employeeService.search("ivan")).isEmpty();

        verifyNoInteractions(employeeRepository);
    }

    @Test
    void searchShouldReturnEmptyListForTooShortEmailQuery() {
        assertThat(employeeService.search("a@b")).isEmpty();

        verifyNoInteractions(employeeRepository);
    }

    @Test
    void searchShouldUseEmailPrefixSearch() {
        Employee employee = Employee.builder()
                .id(2)
                .firstName("Anna")
                .lastName("Sidorova")
                .email("anna@example.com")
                .function(EmployeeType.ADMIN)
                .createdAt(Instant.parse("2026-05-11T10:15:30Z"))
                .build();

        when(employeeRepository.searchByEmailPrefix("anna@ex", 10)).thenReturn(List.of(employee));

        List<EmployeeResponseDTO> response = employeeService.search("Anna@Ex");

        assertThat(response)
                .hasSize(1)
                .extracting(EmployeeResponseDTO::getEmail)
                .containsExactly("anna@example.com");
    }
}
