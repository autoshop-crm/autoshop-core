package com.vladko.autoshopcore.employee.service;

import com.vladko.autoshopcore.employee.dto.EmployeeResponseDTO;
import com.vladko.autoshopcore.employee.dto.EmployeeAvailabilityResponseDTO;
import com.vladko.autoshopcore.entities.Employee;
import com.vladko.autoshopcore.entities.EmployeeType;
import com.vladko.autoshopcore.order.repository.EmployeeRepository;
import com.vladko.autoshopcore.order.repository.OrderAvailabilityProjection;
import com.vladko.autoshopcore.order.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class EmployeeServiceImpl implements EmployeeService {

    private static final int SEARCH_LIMIT = 10;
    private static final int AVAILABILITY_SEARCH_LIMIT = 20;
    private static final int MIN_EMAIL_QUERY_LENGTH = 5;
    private static final EnumSet<EmployeeType> DEFAULT_ASSIGNABLE_ROLES = EnumSet.of(EmployeeType.MECHANIC, EmployeeType.MANAGER);
    private static final List<String> BUSY_STATUSES = List.of(
            "WAITING_FOR_VISIT",
            "ACCEPTED",
            "DIAGNOSIS_IN_PROGRESS",
            "WAITING_FOR_OWNER_APPROVAL",
            "WAITING_FOR_PART",
            "REPAIR_IN_PROGRESS",
            "READY_FOR_OWNER"
    );

    private final EmployeeRepository employeeRepository;
    private final OrderRepository orderRepository;

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

    @Override
    @Transactional(readOnly = true)
    public List<EmployeeAvailabilityResponseDTO> searchAvailability(String query,
                                                                   Collection<EmployeeType> roles,
                                                                   Instant plannedVisitAt,
                                                                   Integer slotMinutes,
                                                                   Integer limit) {
        if (plannedVisitAt == null) {
            throw new IllegalArgumentException("plannedVisitAt is required");
        }
        if (slotMinutes == null || slotMinutes <= 0) {
            throw new IllegalArgumentException("slotMinutes must be greater than zero");
        }

        Collection<EmployeeType> effectiveRoles = roles == null || roles.isEmpty() ? DEFAULT_ASSIGNABLE_ROLES : roles;
        int normalizedLimit = limit == null ? AVAILABILITY_SEARCH_LIMIT : Math.min(Math.max(limit, 1), 100);
        List<Employee> candidates = employeeRepository.searchAvailabilityCandidates(query, effectiveRoles, normalizedLimit);
        if (candidates.isEmpty()) {
            return List.of();
        }

        Instant requestedEnd = plannedVisitAt.plus(slotMinutes, ChronoUnit.MINUTES);
        List<Integer> employeeIds = candidates.stream().map(Employee::getId).toList();
        List<OrderAvailabilityProjection> conflicts = orderRepository.findAvailabilityConflicts(employeeIds, BUSY_STATUSES, plannedVisitAt, requestedEnd);
        Map<Integer, List<OrderAvailabilityProjection>> conflictsByEmployeeId = new HashMap<>();
        for (OrderAvailabilityProjection conflict : conflicts) {
            conflictsByEmployeeId.computeIfAbsent(conflict.getEmployeeId(), ignored -> new java.util.ArrayList<>()).add(conflict);
        }

        return candidates.stream()
                .map(employee -> mapAvailability(employee, conflictsByEmployeeId.getOrDefault(employee.getId(), List.of())))
                .sorted(Comparator
                        .comparing(EmployeeAvailabilityResponseDTO::isAvailable).reversed()
                        .thenComparing(EmployeeAvailabilityResponseDTO::getConflictingOrdersCount)
                        .thenComparing(EmployeeAvailabilityResponseDTO::getFunction)
                        .thenComparing(EmployeeAvailabilityResponseDTO::getLastName, Comparator.nullsLast(String::compareToIgnoreCase))
                        .thenComparing(EmployeeAvailabilityResponseDTO::getFirstName, Comparator.nullsLast(String::compareToIgnoreCase))
                        .thenComparing(EmployeeAvailabilityResponseDTO::getId))
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

    private EmployeeAvailabilityResponseDTO mapAvailability(Employee employee, List<OrderAvailabilityProjection> conflicts) {
        OrderAvailabilityProjection firstConflict = conflicts.isEmpty() ? null : conflicts.get(0);
        return EmployeeAvailabilityResponseDTO.builder()
                .id(employee.getId())
                .firstName(employee.getFirstName())
                .lastName(employee.getLastName())
                .email(employee.getEmail())
                .function(employee.getFunction())
                .available(conflicts.isEmpty())
                .conflictingOrdersCount(conflicts.size())
                .availabilityReason(conflicts.isEmpty() ? "FREE" : "HAS_OVERLAPPING_ORDER")
                .nextConflict(firstConflict == null ? null : EmployeeAvailabilityResponseDTO.ConflictSummaryDTO.builder()
                        .orderId(firstConflict.getId())
                        .plannedVisitAt(firstConflict.getPlannedVisitAt())
                        .slotMinutes(firstConflict.getPlannedSlotMinutes())
                        .status(firstConflict.getStatus())
                        .build())
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
