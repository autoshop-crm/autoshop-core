package com.vladko.autoshopcore.client.service;

import com.vladko.autoshopcore.client.dto.*;
import com.vladko.autoshopcore.client.entity.Customer;
import com.vladko.autoshopcore.client.repository.CustomerRepository;
import com.vladko.autoshopcore.customerauth.service.CustomerIdentityLinkService;
import com.vladko.autoshopcore.loyalty.dto.LoyaltyAccountResponseDTO;
import com.vladko.autoshopcore.loyalty.service.CrmLoyaltyFacade;
import com.vladko.autoshopcore.loyalty.service.LoyaltyService;
import com.vladko.autoshopcore.order.approval.dto.OrderApprovalRequestResponseDTO;
import com.vladko.autoshopcore.order.approval.entity.OrderApprovalRequestStatus;
import com.vladko.autoshopcore.order.approval.service.OrderApprovalService;
import com.vladko.autoshopcore.order.dto.OrderResponseDTO;
import com.vladko.autoshopcore.order.service.OrderService;
import com.vladko.autoshopcore.security.AuthenticatedUser;
import com.vladko.autoshopcore.vehicle.dto.VehicleResponseDTO;
import com.vladko.autoshopcore.vehicle.service.VehicleService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;
import java.util.Locale;

@Service
@RequiredArgsConstructor
public class CustomerSelfServiceImpl implements CustomerSelfService {

    private final CustomerRepository customerRepository;
    private final CustomerIdentityLinkService customerIdentityLinkService;
    private final OrderService orderService;
    private final VehicleService vehicleService;
    private final LoyaltyService loyaltyService;
    private final CrmLoyaltyFacade crmLoyaltyFacade;
    private final OrderApprovalService orderApprovalService;

    @Override
    @Transactional(readOnly = true)
    public CustomerResponseDTO getCurrentCustomer() {
        return mapToResponse(currentCustomer());
    }

    @Override
    @Transactional
    public CustomerResponseDTO updateCurrentCustomer(CustomerUpdateDTO dto) {
        Customer customer = currentCustomer();
        if (dto.getFirstName() != null && !dto.getFirstName().isBlank()) {
            customer.setFirstName(dto.getFirstName().trim());
        }
        if (dto.getLastName() != null && !dto.getLastName().isBlank()) {
            customer.setLastName(dto.getLastName().trim());
        }
        if (dto.getPhoneNumber() != null && !dto.getPhoneNumber().isBlank()) {
            customer.setPhoneNumber(dto.getPhoneNumber().trim());
        }
        if (dto.getEmail() != null && !dto.getEmail().isBlank()
                && !dto.getEmail().trim().equalsIgnoreCase(customer.getEmail())) {
            throw new IllegalArgumentException("Customer email change must go through auth flow");
        }
        return mapToResponse(customerRepository.save(customer));
    }

    @Override
    @Transactional(readOnly = true)
    public List<OrderResponseDTO> getCurrentCustomerOrders() {
        Customer customer = currentCustomer();
        return orderService.getAllByCustomerId(customer.getId());
    }

    @Override
    @Transactional(readOnly = true)
    public List<VehicleResponseDTO> getCurrentCustomerVehicles() {
        Customer customer = currentCustomer();
        return vehicleService.getAllByCustomerId(customer.getId());
    }

    @Override
    @Transactional
    public CustomerLoyaltyOverviewDTO getCurrentCustomerLoyalty() {
        Customer customer = currentCustomer();
        LoyaltyAccountResponseDTO account = loyaltyService.getOrCreateAccountByCustomerId(customer.getId());
        return CustomerLoyaltyOverviewDTO.builder()
                .account(account)
                .recentTransactions(loyaltyService.getTransactions(account.getId()).stream().limit(20).toList())
                .tiers(loyaltyService.getTiers())
                .build();
    }

    @Override
    @Transactional
    public CustomerSelfServiceDashboardDTO getCurrentCustomerDashboard() {
        Customer customer = currentCustomer();
        List<OrderResponseDTO> orders = orderService.getAllByCustomerId(customer.getId());
        List<OrderApprovalRequestResponseDTO> pendingApprovals = orders.stream()
                .flatMap(order -> orderApprovalService.getByOrderId(order.getId()).stream())
                .filter(approval -> approval.getRequestStatus() == OrderApprovalRequestStatus.OPEN)
                .sorted(Comparator.comparing(OrderApprovalRequestResponseDTO::getRequestedAt, Comparator.nullsLast(Comparator.reverseOrder())))
                .toList();
        return CustomerSelfServiceDashboardDTO.builder()
                .customer(mapToResponse(customer))
                .recentOrders(orders.stream().limit(10).toList())
                .pendingApprovals(pendingApprovals)
                .vehicles(vehicleService.getAllByCustomerId(customer.getId()))
                .loyalty(getCurrentCustomerLoyalty())
                .loyaltySettings(crmLoyaltyFacade.getSettings())
                .build();
    }

    private Customer currentCustomer() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof AuthenticatedUser authenticatedUser)) {
            throw new com.vladko.autoshopcore.client.exception.CustomerNotFoundException("current authenticated customer");
        }
        return customerIdentityLinkService.getRequiredCurrentCustomer(authenticatedUser.userId(), authenticatedUser.email());
    }

    private CustomerResponseDTO mapToResponse(Customer customer) {
        return CustomerResponseDTO.builder()
                .id(customer.getId())
                .firstName(customer.getFirstName())
                .lastName(customer.getLastName())
                .phoneNumber(customer.getPhoneNumber())
                .email(customer.getEmail())
                .emailVerified(customer.getEmailVerified())
                .createdAt(customer.getCreatedAt())
                .updatedAt(customer.getUpdatedAt())
                .build();
    }
}
