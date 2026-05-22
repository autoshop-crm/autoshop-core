package com.vladko.autoshopcore.employee.dto;

import com.vladko.autoshopcore.entities.EmployeeType;
import com.vladko.autoshopcore.order.entity.OrderStatus;
import lombok.Builder;
import lombok.Data;

import java.time.Instant;

@Data
@Builder
public class EmployeeAvailabilityResponseDTO {

    private Integer id;
    private String firstName;
    private String lastName;
    private String email;
    private EmployeeType function;
    private boolean available;
    private int conflictingOrdersCount;
    private String availabilityReason;
    private ConflictSummaryDTO nextConflict;

    @Data
    @Builder
    public static class ConflictSummaryDTO {
        private Integer orderId;
        private Instant plannedVisitAt;
        private Integer slotMinutes;
        private OrderStatus status;
    }
}
