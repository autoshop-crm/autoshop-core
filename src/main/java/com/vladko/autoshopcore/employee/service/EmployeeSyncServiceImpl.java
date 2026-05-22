package com.vladko.autoshopcore.employee.service;

import com.vladko.autoshopcore.employee.dto.EmployeeResponseDTO;
import com.vladko.autoshopcore.employee.dto.EmployeeSyncRequestDTO;
import com.vladko.autoshopcore.entities.Employee;
import com.vladko.autoshopcore.entities.EmployeeType;
import com.vladko.autoshopcore.order.repository.EmployeeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.Locale;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class EmployeeSyncServiceImpl implements EmployeeSyncService {

    private static final Comparator<EmployeeType> ROLE_PRIORITY = Comparator.comparingInt(EmployeeSyncServiceImpl::priority);

    private final EmployeeRepository employeeRepository;

    @Override
    @Transactional
    public EmployeeResponseDTO sync(EmployeeSyncRequestDTO request) {
        String normalizedEmail = normalizeRequiredEmail(request.email());
        EmployeeType employeeType = resolveEmployeeType(request.roles());

        Employee employee = employeeRepository.findByEmail(normalizedEmail)
                .orElseGet(Employee::new);

        employee.setEmail(normalizedEmail);
        employee.setFirstName(normalizeName(request.firstName(), employee.getFirstName(), "Unknown"));
        employee.setLastName(normalizeName(request.lastName(), employee.getLastName(), "User"));
        employee.setFunction(employeeType);

        Employee saved = employeeRepository.save(employee);
        return EmployeeResponseDTO.builder()
                .id(saved.getId())
                .firstName(saved.getFirstName())
                .lastName(saved.getLastName())
                .email(saved.getEmail())
                .function(saved.getFunction())
                .createdAt(saved.getCreatedAt())
                .build();
    }

    private static int priority(EmployeeType type) {
        return switch (type) {
            case ADMIN -> 0;
            case MANAGER -> 1;
            case RECEPTIONIST -> 2;
            case MECHANIC -> 3;
        };
    }

    private EmployeeType resolveEmployeeType(Set<String> roles) {
        if (roles == null || roles.isEmpty()) {
            throw new IllegalArgumentException("At least one staff role is required");
        }

        return roles.stream()
                .map(this::toEmployeeType)
                .filter(type -> type != null)
                .min(ROLE_PRIORITY)
                .orElseThrow(() -> new IllegalArgumentException("No staff roles found for employee sync"));
    }

    private EmployeeType toEmployeeType(String role) {
        if (role == null) {
            return null;
        }
        return switch (role.trim().toUpperCase(Locale.ROOT)) {
            case "ADMIN" -> EmployeeType.ADMIN;
            case "MANAGER" -> EmployeeType.MANAGER;
            case "RECEPTIONIST" -> EmployeeType.RECEPTIONIST;
            case "MECHANIC" -> EmployeeType.MECHANIC;
            default -> null;
        };
    }

    private String normalizeRequiredEmail(String email) {
        if (email == null || email.trim().isEmpty()) {
            throw new IllegalArgumentException("Email must not be blank");
        }
        return email.trim().toLowerCase(Locale.ROOT);
    }

    private String normalizeName(String value, String currentValue, String fallback) {
        if (value != null && !value.trim().isEmpty()) {
            return value.trim();
        }
        if (currentValue != null && !currentValue.trim().isEmpty()) {
            return currentValue;
        }
        return fallback;
    }
}
