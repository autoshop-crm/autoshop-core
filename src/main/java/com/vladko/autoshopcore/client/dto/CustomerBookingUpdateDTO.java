package com.vladko.autoshopcore.client.dto;

import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.List;

@Builder
@AllArgsConstructor
@NoArgsConstructor
@Data
public class CustomerBookingUpdateDTO {
    @Future
    private Instant plannedVisitAt;

    private Integer plannedSlotMinutes;

    @Size(max = 1000)
    private String problem;

    @Size(max = 2000)
    private String intakeNotes;

    private List<Integer> selectedServiceIds;
}
