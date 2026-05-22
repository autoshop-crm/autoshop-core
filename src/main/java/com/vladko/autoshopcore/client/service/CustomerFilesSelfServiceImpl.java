package com.vladko.autoshopcore.client.service;

import com.vladko.autoshopcore.client.dto.CustomerFileDownloadUrlResponseDTO;
import com.vladko.autoshopcore.client.dto.CustomerFileMetadataDTO;
import com.vladko.autoshopcore.client.entity.Customer;
import com.vladko.autoshopcore.client.files.FilesGateway;
import com.vladko.autoshopcore.customerauth.service.CustomerIdentityLinkService;
import com.vladko.autoshopcore.order.dto.OrderResponseDTO;
import com.vladko.autoshopcore.order.service.OrderService;
import com.vladko.autoshopcore.security.AuthenticatedUser;
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
public class CustomerFilesSelfServiceImpl implements CustomerFilesSelfService {

    private final CustomerIdentityLinkService customerIdentityLinkService;
    private final VehicleService vehicleService;
    private final OrderService orderService;
    private final FilesGateway filesGateway;

    @Override
    @Transactional(readOnly = true)
    public List<CustomerFileMetadataDTO> getCurrentCustomerDocuments() {
        Customer customer = currentCustomer();
        return map(filesGateway.listByOwner("CUSTOMER", String.valueOf(customer.getId())));
    }

    @Override
    @Transactional(readOnly = true)
    public List<CustomerFileMetadataDTO> getCurrentCustomerVehicleDocuments(Integer vehicleId) {
        Customer customer = currentCustomer();
        VehicleResponseDTO vehicle = vehicleService.getById(vehicleId);
        requireVehicleOwnership(vehicle, customer.getId());
        return map(filesGateway.listByOwner("VEHICLE", String.valueOf(vehicleId)));
    }

    @Override
    @Transactional(readOnly = true)
    public List<CustomerFileMetadataDTO> getCurrentCustomerOrderDocuments(Integer orderId) {
        currentCustomer();
        OrderResponseDTO order = orderService.getById(orderId);
        return map(filesGateway.listByOwner("ORDER", String.valueOf(order.getId())));
    }

    @Override
    @Transactional(readOnly = true)
    public CustomerFileDownloadUrlResponseDTO getPresignedDownloadUrl(String fileId) {
        currentCustomer();
        var file = filesGateway.getById(fileId);
        assertAccessible(file.getOwnerType(), file.getOwnerId());
        var response = filesGateway.createPresignedDownloadUrl(fileId);
        return CustomerFileDownloadUrlResponseDTO.builder().url(response.getUrl()).build();
    }

    private List<CustomerFileMetadataDTO> map(List<com.vladko.autoshopcore.client.files.ExternalFileMetadataDTO> files) {
        return files.stream().map(file -> CustomerFileMetadataDTO.builder()
                .fileId(file.getFileId())
                .filename(file.getFilename())
                .category(file.getCategory())
                .ownerType(file.getOwnerType())
                .ownerId(file.getOwnerId())
                .contentType(file.getContentType())
                .sizeBytes(file.getSizeBytes())
                .status(file.getStatus())
                .createdAt(file.getCreatedAt())
                .build()).toList();
    }

    private void assertAccessible(String ownerType, String ownerId) {
        Customer customer = currentCustomer();
        switch (ownerType) {
            case "CUSTOMER" -> {
                if (!String.valueOf(customer.getId()).equals(ownerId)) {
                    throw new AccessDeniedException("Customer cannot access this file");
                }
            }
            case "VEHICLE" -> {
                VehicleResponseDTO vehicle = vehicleService.getById(Integer.valueOf(ownerId));
                requireVehicleOwnership(vehicle, customer.getId());
            }
            case "ORDER" -> orderService.getById(Integer.valueOf(ownerId));
            default -> throw new AccessDeniedException("Customer cannot access this file");
        }
    }

    private void requireVehicleOwnership(VehicleResponseDTO vehicle, Integer customerId) {
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
