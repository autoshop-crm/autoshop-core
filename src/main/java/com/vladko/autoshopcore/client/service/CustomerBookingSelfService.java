package com.vladko.autoshopcore.client.service;

import com.vladko.autoshopcore.client.dto.CustomerBookingCreateDTO;
import com.vladko.autoshopcore.client.dto.CustomerBookingServiceCatalogItemDTO;
import com.vladko.autoshopcore.client.dto.CustomerBookingUpdateDTO;
import com.vladko.autoshopcore.order.dto.OrderResponseDTO;

import java.util.List;

public interface CustomerBookingSelfService {
    List<CustomerBookingServiceCatalogItemDTO> getAvailableServices();
    OrderResponseDTO createBooking(CustomerBookingCreateDTO dto);
    OrderResponseDTO updateBooking(Integer orderId, CustomerBookingUpdateDTO dto);
    OrderResponseDTO cancelBooking(Integer orderId);
    OrderResponseDTO getCurrentCustomerOrder(Integer orderId);
}
