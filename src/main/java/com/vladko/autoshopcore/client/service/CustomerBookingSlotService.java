package com.vladko.autoshopcore.client.service;

import com.vladko.autoshopcore.client.dto.CustomerBookingSlotResponseDTO;
import com.vladko.autoshopcore.client.dto.CustomerBookingAvailabilityResponseDTO;

import java.time.LocalDate;
import java.util.List;

public interface CustomerBookingSlotService {
    List<CustomerBookingSlotResponseDTO> lookupSlots(Integer vehicleId,
                                                     List<Integer> serviceIds,
                                                     LocalDate dateFrom,
                                                     Integer days,
                                                     Integer slotMinutes);

    List<CustomerBookingAvailabilityResponseDTO> lookupAvailability(Integer vehicleId,
                                                                   List<Integer> serviceIds,
                                                                   LocalDate from,
                                                                   Integer days);
}
