package com.vladko.autoshopcore.client.dto;

import lombok.Builder;
import lombok.Value;

import java.time.LocalDate;

@Value
@Builder
public class CustomerBookingAvailabilityResponseDTO {
    LocalDate date;
    boolean available;
    String reason;
}
