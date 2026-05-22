package com.vladko.autoshopcore.client.dto;

import lombok.Builder;
import lombok.Value;

import java.time.Instant;

@Value
@Builder
public class CustomerBookingSlotResponseDTO {
    Instant startAt;
    Integer slotMinutes;
    boolean available;
    Integer availableEmployeeCount;
}
