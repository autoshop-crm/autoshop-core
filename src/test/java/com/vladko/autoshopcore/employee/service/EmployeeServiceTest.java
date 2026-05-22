package com.vladko.autoshopcore.employee.service;

import com.vladko.autoshopcore.employee.dto.EmployeeResponseDTO;
import com.vladko.autoshopcore.order.entity.OrderStatus;
import com.vladko.autoshopcore.entities.Employee;
import com.vladko.autoshopcore.entities.EmployeeType;
import com.vladko.autoshopcore.order.repository.EmployeeRepository;
import com.vladko.autoshopcore.order.repository.OrderAvailabilityProjection;
import com.vladko.autoshopcore.order.repository.OrderRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EmployeeServiceTest {

    @Mock
    private EmployeeRepository employeeRepository;

    @Mock
    private OrderRepository orderRepository;

    private EmployeeService employeeService;

    @BeforeEach
    void setUp() {
        employeeService = new EmployeeServiceImpl(employeeRepository, orderRepository);
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

    @Test
    void searchAvailabilityShouldReturnFreeEmployeesBeforeBusyEmployees() {
        Employee busy = Employee.builder()
                .id(2)
                .firstName("Busy")
                .lastName("Mechanic")
                .email("busy@example.com")
                .function(EmployeeType.MECHANIC)
                .build();
        Employee free = Employee.builder()
                .id(1)
                .firstName("Free")
                .lastName("Mechanic")
                .email("free@example.com")
                .function(EmployeeType.MECHANIC)
                .build();

        when(employeeRepository.searchAvailabilityCandidates(null, List.of(EmployeeType.MECHANIC), 20)).thenReturn(List.of(busy, free));
        when(orderRepository.findAvailabilityConflicts(any(), any(), any(), any())).thenReturn(List.of(new Projection(11, 2, Instant.parse("2026-05-20T10:00:00Z"), 60, OrderStatus.ACCEPTED)));

        var response = employeeService.searchAvailability(null, List.of(EmployeeType.MECHANIC), Instant.parse("2026-05-20T10:00:00Z"), 60, 20);

        assertThat(response).hasSize(2);
        assertThat(response.get(0).getId()).isEqualTo(1);
        assertThat(response.get(0).isAvailable()).isTrue();
        assertThat(response.get(1).getId()).isEqualTo(2);
        assertThat(response.get(1).isAvailable()).isFalse();
        assertThat(response.get(1).getConflictingOrdersCount()).isEqualTo(1);
    }

    private record Projection(Integer id, Integer employeeId, Instant plannedVisitAt, Integer plannedSlotMinutes,
                              OrderStatus status) implements OrderAvailabilityProjection {
        @Override public Integer getId() { return id; }
        @Override public Integer getEmployeeId() { return employeeId; }
        @Override public Instant getPlannedVisitAt() { return plannedVisitAt; }
        @Override public Integer getPlannedSlotMinutes() { return plannedSlotMinutes; }
        @Override public OrderStatus getStatus() { return status; }
    }
}
