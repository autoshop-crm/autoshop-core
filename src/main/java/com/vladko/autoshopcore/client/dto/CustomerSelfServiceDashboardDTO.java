package com.vladko.autoshopcore.client.dto;

import com.vladko.autoshopcore.loyalty.dto.LoyaltySettingsResponseDTO;
import com.vladko.autoshopcore.order.approval.dto.OrderApprovalRequestResponseDTO;
import com.vladko.autoshopcore.order.dto.OrderResponseDTO;
import com.vladko.autoshopcore.vehicle.dto.VehicleResponseDTO;
import lombok.Builder;
import lombok.Value;

import java.util.List;

@Value
@Builder
public class CustomerSelfServiceDashboardDTO {
    CustomerResponseDTO customer;
    List<OrderResponseDTO> recentOrders;
    List<OrderApprovalRequestResponseDTO> pendingApprovals;
    List<VehicleResponseDTO> vehicles;
    CustomerLoyaltyOverviewDTO loyalty;
    LoyaltySettingsResponseDTO loyaltySettings;
}
