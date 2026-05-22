package com.vladko.autoshopcore.client.service;

import com.vladko.autoshopcore.client.dto.CustomerLoyaltyOverviewDTO;
import com.vladko.autoshopcore.client.dto.CustomerResponseDTO;
import com.vladko.autoshopcore.client.dto.CustomerSelfServiceDashboardDTO;
import com.vladko.autoshopcore.client.dto.CustomerUpdateDTO;
import com.vladko.autoshopcore.order.dto.OrderResponseDTO;
import com.vladko.autoshopcore.vehicle.dto.VehicleResponseDTO;

import java.util.List;

public interface CustomerSelfService {
    CustomerResponseDTO getCurrentCustomer();
    CustomerResponseDTO updateCurrentCustomer(CustomerUpdateDTO dto);
    List<OrderResponseDTO> getCurrentCustomerOrders();
    List<VehicleResponseDTO> getCurrentCustomerVehicles();
    CustomerLoyaltyOverviewDTO getCurrentCustomerLoyalty();
    CustomerSelfServiceDashboardDTO getCurrentCustomerDashboard();
}
