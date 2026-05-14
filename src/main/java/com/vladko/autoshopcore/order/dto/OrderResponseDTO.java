package com.vladko.autoshopcore.order.dto;

import com.vladko.autoshopcore.order.entity.BookingChannel;
import com.vladko.autoshopcore.order.entity.CancellationReason;
import com.vladko.autoshopcore.order.entity.LegacyOrderStatus;
import com.vladko.autoshopcore.order.entity.OrderStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

@Builder
@AllArgsConstructor
@NoArgsConstructor
@Data
public class OrderResponseDTO {
    private Integer id;
    private Integer customerId;
    private Integer vehicleId;
    private Integer employeeId;
    private String problem;
    private OrderStatus status;
    private OrderStatus crmStatus;
    private LegacyOrderStatus legacyStatus;
    private Instant plannedVisitAt;
    private Integer plannedSlotMinutes;
    private BookingChannel bookingChannel;
    private String intakeNotes;
    private Boolean requiresOwnerApprovalForEveryExtraWork;
    private Boolean plannedDropOff;
    private Instant checkedInAt;
    private Instant readyForOwnerAt;
    private Instant handedOverAt;
    private Instant cancelledAt;
    private CancellationReason cancellationReason;
    private BigDecimal laborTotal;
    private BigDecimal partsTotal;
    private BigDecimal costsTotal;
    private BigDecimal manualDiscountAmount;
    private BigDecimal pointsDiscountAmount;
    private Integer loyaltyPointsSpent;
    private BigDecimal discountAmount;
    private BigDecimal finalAmount;
    private Instant createdAt;
    private Instant updatedAt;
    private Instant completedAt;
    private List<OrderServiceLineDTO> serviceLines;
}
