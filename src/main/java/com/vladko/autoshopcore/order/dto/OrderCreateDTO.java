package com.vladko.autoshopcore.order.dto;

import com.vladko.autoshopcore.order.entity.BookingChannel;
import jakarta.validation.constraints.NotNull;
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
public class OrderCreateDTO {
    @NotNull
    private Integer customerId;

    @NotNull
    private Integer vehicleId;

    private Integer employeeId;

    @Size(max = 1000)
    private String problem;

    private Instant plannedVisitAt;

    private Integer plannedSlotMinutes;

    private BookingChannel bookingChannel;

    @Size(max = 2000)
    private String intakeNotes;

    private Boolean requiresOwnerApprovalForEveryExtraWork;

    private Boolean immediateDropOff;

    private List<Integer> selectedServiceIds;
}
