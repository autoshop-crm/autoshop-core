package com.vladko.autoshopcore.client.service;

import com.vladko.autoshopcore.client.dto.CustomerBookingSlotResponseDTO;

import java.time.LocalDate;
import java.util.List;

public interface CustomerBookingSlotService {
    List<CustomerBookingSlotResponseDTO> lookupSlots(Integer vehicleId,
                                                     List<Integer> serviceIds,
                                                     LocalDate dateFrom,
                                                     Integer days,
                                                     Integer slotMinutes);
}
