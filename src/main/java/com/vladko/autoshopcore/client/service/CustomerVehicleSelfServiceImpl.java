package com.vladko.autoshopcore.client.service;

import com.vladko.autoshopcore.client.dto.CustomerVehicleCreateDTO;
import com.vladko.autoshopcore.client.dto.CustomerVehicleUpdateDTO;
import com.vladko.autoshopcore.client.entity.Customer;
import com.vladko.autoshopcore.customerauth.service.CustomerIdentityLinkService;
import com.vladko.autoshopcore.order.entity.OrderStatus;
import com.vladko.autoshopcore.order.repository.OrderRepository;
import com.vladko.autoshopcore.security.AuthenticatedUser;
import com.vladko.autoshopcore.vehicle.dto.VehicleCreateDTO;
import com.vladko.autoshopcore.vehicle.dto.VehicleResponseDTO;
import com.vladko.autoshopcore.vehicle.dto.VehicleUpdateDTO;
import com.vladko.autoshopcore.vehicle.service.VehicleService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.EnumSet;
import java.util.List;

@Service
@RequiredArgsConstructor
public class CustomerVehicleSelfServiceImpl implements CustomerVehicleSelfService {

    private static final EnumSet<OrderStatus> ACTIVE_VEHICLE_ORDER_STATUSES = EnumSet.of(
            OrderStatus.WAITING_FOR_VISIT,
            OrderStatus.ACCEPTED,
            OrderStatus.DIAGNOSIS_IN_PROGRESS,
            OrderStatus.WAITING_FOR_OWNER_APPROVAL,
            OrderStatus.WAITING_FOR_PART,
            OrderStatus.REPAIR_IN_PROGRESS,
            OrderStatus.READY_FOR_OWNER
    );

    private final CustomerIdentityLinkService customerIdentityLinkService;
    private final VehicleService vehicleService;
    private final OrderRepository orderRepository;

    @Override
    @Transactional(readOnly = true)
    public List<VehicleResponseDTO> getCurrentCustomerVehicles() {
        Customer customer = currentCustomer();
        return vehicleService.getAllByCustomerId(customer.getId());
    }

    @Override
    @Transactional(readOnly = true)
    public VehicleResponseDTO getCurrentCustomerVehicle(Integer vehicleId) {
        Customer customer = currentCustomer();
        VehicleResponseDTO vehicle = vehicleService.getById(vehicleId);
        requireOwnership(vehicle, customer.getId());
        return vehicle;
    }

    @Override
    @Transactional
    public VehicleResponseDTO createVehicle(CustomerVehicleCreateDTO dto) {
        Customer customer = currentCustomer();
        return vehicleService.create(VehicleCreateDTO.builder()
                .customerId(customer.getId())
                .brand(dto.getBrand())
                .model(dto.getModel())
                .vin(dto.getVin())
                .licensePlate(dto.getLicensePlate())
                .build());
    }

    @Override
    @Transactional
    public VehicleResponseDTO updateVehicle(Integer vehicleId, CustomerVehicleUpdateDTO dto) {
        Customer customer = currentCustomer();
        VehicleResponseDTO vehicle = vehicleService.getById(vehicleId);
        requireOwnership(vehicle, customer.getId());
        return vehicleService.update(vehicleId, VehicleUpdateDTO.builder()
                .brand(dto.getBrand())
                .model(dto.getModel())
                .vin(dto.getVin())
                .licensePlate(dto.getLicensePlate())
                .build());
    }

    @Override
    @Transactional
    public void deleteVehicle(Integer vehicleId) {
        Customer customer = currentCustomer();
        VehicleResponseDTO vehicle = vehicleService.getById(vehicleId);
        requireOwnership(vehicle, customer.getId());
        boolean hasActiveOrders = orderRepository.findAllByVehicleIdOrderByIdDesc(vehicleId).stream()
                .anyMatch(order -> ACTIVE_VEHICLE_ORDER_STATUSES.contains(order.getStatus()));
        if (hasActiveOrders) {
            throw new IllegalArgumentException("Vehicle with active orders cannot be deleted");
        }
        vehicleService.delete(vehicleId);
    }

    private void requireOwnership(VehicleResponseDTO vehicle, Integer customerId) {
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
