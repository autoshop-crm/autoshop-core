package com.vladko.autoshopcore.client.service;

import com.vladko.autoshopcore.client.dto.CustomerBookingCreateDTO;
import com.vladko.autoshopcore.client.dto.CustomerBookingServiceCatalogItemDTO;
import com.vladko.autoshopcore.client.dto.CustomerBookingUpdateDTO;
import com.vladko.autoshopcore.client.entity.Customer;
import com.vladko.autoshopcore.customerauth.service.CustomerIdentityLinkService;
import com.vladko.autoshopcore.order.dto.OrderCreateDTO;
import com.vladko.autoshopcore.order.dto.OrderResponseDTO;
import com.vladko.autoshopcore.order.dto.OrderStatusUpdateDTO;
import com.vladko.autoshopcore.order.dto.OrderUpdateDTO;
import com.vladko.autoshopcore.order.entity.BookingChannel;
import com.vladko.autoshopcore.order.entity.OrderStatus;
import com.vladko.autoshopcore.order.service.OrderService;
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

import java.util.List;

@Service
@RequiredArgsConstructor
public class CustomerBookingSelfServiceImpl implements CustomerBookingSelfService {

    private final CustomerIdentityLinkService customerIdentityLinkService;
    private final VehicleService vehicleService;
    private final OrderService orderService;
    private final ServiceCatalogService serviceCatalogService;

    @Override
    @Transactional(readOnly = true)
    public List<CustomerBookingServiceCatalogItemDTO> getAvailableServices() {
        return serviceCatalogService.getServices(true, null).stream()
                .map(item -> CustomerBookingServiceCatalogItemDTO.builder()
                        .id(item.getId())
                        .name(item.getName())
                        .description(item.getDescription())
                        .basePrice(item.getBasePrice())
                        .categoryId(item.getCategoryId())
                        .categoryName(item.getCategoryName())
                        .defaultDurationMinutes(item.getDefaultDurationMinutes())
                        .build())
                .toList();
    }

    @Override
    @Transactional
    public OrderResponseDTO createBooking(CustomerBookingCreateDTO dto) {
        Customer customer = currentCustomer();
        requireVehicleOwnership(dto.getVehicleId(), customer.getId());
        return orderService.createForCustomer(OrderCreateDTO.builder()
                .customerId(customer.getId())
                .vehicleId(dto.getVehicleId())
                .problem(dto.getProblem())
                .plannedVisitAt(dto.getPlannedVisitAt())
                .plannedSlotMinutes(dto.getPlannedSlotMinutes())
                .bookingChannel(BookingChannel.WEB)
                .intakeNotes(dto.getIntakeNotes())
                .selectedServiceIds(dto.getSelectedServiceIds())
                .build());
    }

    @Override
    @Transactional
    public OrderResponseDTO updateBooking(Integer orderId, CustomerBookingUpdateDTO dto) {
        currentCustomer();
        return orderService.updateForCustomer(orderId, OrderUpdateDTO.builder()
                .problem(dto.getProblem())
                .plannedVisitAt(dto.getPlannedVisitAt())
                .plannedSlotMinutes(dto.getPlannedSlotMinutes())
                .intakeNotes(dto.getIntakeNotes())
                .selectedServiceIds(dto.getSelectedServiceIds())
                .build());
    }

    @Override
    @Transactional
    public OrderResponseDTO cancelBooking(Integer orderId) {
        currentCustomer();
        return orderService.updateStatus(orderId, OrderStatusUpdateDTO.builder()
                .status(OrderStatus.CANCELLED_BY_CUSTOMER)
                .build());
    }

    @Override
    @Transactional(readOnly = true)
    public OrderResponseDTO getCurrentCustomerOrder(Integer orderId) {
        currentCustomer();
        return orderService.getById(orderId);
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
