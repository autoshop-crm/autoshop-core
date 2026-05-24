package com.vladko.autoshopcore.client.service;

import com.vladko.autoshopcore.client.dto.CustomerBookingAvailabilityResponseDTO;
import com.vladko.autoshopcore.client.dto.CustomerBookingSlotResponseDTO;
import com.vladko.autoshopcore.client.entity.Customer;
import com.vladko.autoshopcore.customerauth.service.CustomerIdentityLinkService;
import com.vladko.autoshopcore.employee.service.EmployeeService;
import com.vladko.autoshopcore.entities.EmployeeType;
import com.vladko.autoshopcore.security.AuthenticatedUser;
import com.vladko.autoshopcore.servicecatalog.service.ServiceCatalogService;
import com.vladko.autoshopcore.vehicle.dto.VehicleResponseDTO;
import com.vladko.autoshopcore.vehicle.service.VehicleService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneOffset;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class CustomerBookingSlotServiceImpl implements CustomerBookingSlotService {

    private static final int DEFAULT_DAYS = 7;
    private static final int DEFAULT_SLOT_MINUTES = 60;
    private static final int SLOT_STEP_MINUTES = 60;
    private static final LocalTime DAY_START = LocalTime.of(9, 0);
    private static final LocalTime DAY_END_EXCLUSIVE = LocalTime.of(18, 0);

    private final CustomerIdentityLinkService customerIdentityLinkService;
    private final VehicleService vehicleService;
    private final EmployeeService employeeService;
    private final ServiceCatalogService serviceCatalogService;

    @Override
    @Transactional(readOnly = true)
    public List<CustomerBookingSlotResponseDTO> lookupSlots(Integer vehicleId, List<Integer> serviceIds, LocalDate dateFrom, Integer days, Integer slotMinutes) {
        Customer customer = currentCustomer();
        requireVehicleOwnership(vehicleId, customer.getId());
        LocalDate effectiveDateFrom = dateFrom == null ? LocalDate.now(ZoneOffset.UTC) : dateFrom;
        int effectiveDays = days == null ? DEFAULT_DAYS : Math.min(Math.max(days, 1), 14);
        int effectiveSlotMinutes = resolveSlotMinutes(serviceIds, slotMinutes);

        java.util.ArrayList<CustomerBookingSlotResponseDTO> result = new java.util.ArrayList<>();
        for (int day = 0; day < effectiveDays; day++) {
            LocalDate currentDate = effectiveDateFrom.plusDays(day);
            for (LocalTime time = DAY_START; time.isBefore(DAY_END_EXCLUSIVE); time = time.plusMinutes(SLOT_STEP_MINUTES)) {
                Instant startAt = currentDate.atTime(time).toInstant(ZoneOffset.UTC);
                var availability = employeeService.searchAvailability(null,
                        List.of(EmployeeType.MECHANIC, EmployeeType.MANAGER),
                        startAt,
                        effectiveSlotMinutes,
                        20);
                long availableCount = availability.stream().filter(item -> item.isAvailable()).count();
                result.add(CustomerBookingSlotResponseDTO.builder()
                        .startAt(startAt)
                        .slotMinutes(effectiveSlotMinutes)
                        .available(availableCount > 0)
                        .availableEmployeeCount((int) availableCount)
                        .build());
            }
        }
        return result;
    }

    @Override
    @Transactional(readOnly = true)
    public List<CustomerBookingAvailabilityResponseDTO> lookupAvailability(Integer vehicleId,
                                                                          List<Integer> serviceIds,
                                                                          LocalDate from,
                                                                          Integer days) {
        LocalDate effectiveFrom = from == null ? LocalDate.now(ZoneOffset.UTC) : from;
        int effectiveDays = days == null ? DEFAULT_DAYS : Math.min(Math.max(days, 1), 30);
        List<CustomerBookingSlotResponseDTO> slots = lookupSlots(vehicleId, serviceIds, effectiveFrom, effectiveDays, null);

        Map<LocalDate, Boolean> availabilityByDay = new LinkedHashMap<>();
        for (int day = 0; day < effectiveDays; day++) {
            availabilityByDay.put(effectiveFrom.plusDays(day), false);
        }
        for (CustomerBookingSlotResponseDTO slot : slots) {
            LocalDate slotDate = slot.getStartAt().atOffset(ZoneOffset.UTC).toLocalDate();
            if (slot.isAvailable()) {
                availabilityByDay.put(slotDate, true);
            }
        }

        return availabilityByDay.entrySet().stream()
                .map(entry -> CustomerBookingAvailabilityResponseDTO.builder()
                        .date(entry.getKey())
                        .available(entry.getValue())
                        .reason(entry.getValue() ? null : "FULL")
                        .build())
                .toList();
    }

    private int resolveSlotMinutes(List<Integer> serviceIds, Integer slotMinutes) {
        if (slotMinutes != null && slotMinutes > 0) {
            return slotMinutes;
        }
        if (serviceIds == null || serviceIds.isEmpty()) {
            return DEFAULT_SLOT_MINUTES;
        }
        var services = serviceCatalogService.getServices(true, null).stream()
                .filter(item -> serviceIds.contains(item.getId()))
                .toList();
        if (services.size() != serviceIds.size()) {
            throw new IllegalArgumentException("Selected services must be active and available for booking");
        }
        int totalMinutes = services.stream()
                .map(item -> item.getDefaultDurationMinutes() == null || item.getDefaultDurationMinutes() <= 0 ? DEFAULT_SLOT_MINUTES : item.getDefaultDurationMinutes())
                .reduce(0, Integer::sum);
        return Math.max(totalMinutes, DEFAULT_SLOT_MINUTES);
    }

    private void requireVehicleOwnership(Integer vehicleId, Integer customerId) {
        VehicleResponseDTO vehicle = vehicleService.getById(vehicleId);
        if (!vehicle.getCustomerId().equals(customerId)) {
            throw new AccessDeniedException("Customer cannot access this vehicle");
        }
    }

    private Customer currentCustomer() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof AuthenticatedUser authenticatedUser)) {
            throw new com.vladko.autoshopcore.client.exception.CustomerNotFoundException("current authenticated customer");
        }
        return customerIdentityLinkService.getRequiredCurrentCustomer(authenticatedUser.userId(), authenticatedUser.email());
    }
}
