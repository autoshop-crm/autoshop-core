package com.vladko.autoshopcore.employee.service;

import com.vladko.autoshopcore.employee.dto.EmployeeResponseDTO;
import com.vladko.autoshopcore.entities.Employee;
import com.vladko.autoshopcore.order.repository.EmployeeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

@Service
@RequiredArgsConstructor
public class EmployeeServiceImpl implements EmployeeService {

    private static final int SEARCH_LIMIT = 10;
    private static final int MIN_EMAIL_QUERY_LENGTH = 5;

    private final EmployeeRepository employeeRepository;

    @Override
    @Transactional(readOnly = true)
    public List<EmployeeResponseDTO> getAll() {
        return employeeRepository.findAllByOrderByIdAsc()
                .stream()
                .map(this::mapToResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<EmployeeResponseDTO> search(String query) {
        String normalizedQuery = normalizeOptionalText(query);
        if (normalizedQuery == null || !normalizedQuery.contains("@")) {
            return Collections.emptyList();
        }

        String normalizedEmailQuery = normalizedQuery.toLowerCase(Locale.ROOT);
        if (normalizedEmailQuery.length() < MIN_EMAIL_QUERY_LENGTH) {
            return Collections.emptyList();
        }

        return employeeRepository.searchByEmailPrefix(normalizedEmailQuery, SEARCH_LIMIT)
                .stream()
                .sorted(Comparator.comparing(Employee::getId))
                .map(this::mapToResponse)
                .toList();
    }

    private EmployeeResponseDTO mapToResponse(Employee employee) {
        return EmployeeResponseDTO.builder()
                .id(employee.getId())
                .firstName(employee.getFirstName())
                .lastName(employee.getLastName())
                .email(employee.getEmail())
                .function(employee.getFunction())
                .createdAt(employee.getCreatedAt())
                .build();
    }

    private String normalizeOptionalText(String value) {
        if (value == null) {
            return null;
        }

        String normalizedValue = value.trim();
        return normalizedValue.isEmpty() ? null : normalizedValue;
    }
}
