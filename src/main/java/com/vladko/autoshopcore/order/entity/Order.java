package com.vladko.autoshopcore.order.entity;

import com.vladko.autoshopcore.client.entity.Customer;
import com.vladko.autoshopcore.entities.Employee;
import com.vladko.autoshopcore.shared.entities.BaseEntity;
import com.vladko.autoshopcore.vehicle.entity.Vehicle;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.ColumnDefault;

import java.math.BigDecimal;
import java.time.Instant;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity(name = "CustomerOrder")
@Table(name = "orders")
public class Order implements BaseEntity<Integer> {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "customer_id", nullable = false)
    private Customer customer;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "vehicle_id", nullable = false)
    private Vehicle vehicle;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "employee_id")
    private Employee employee;

    @Column(name = "problem", nullable = false)
    private String problem;

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 40)
    private OrderStatus status = OrderStatus.NEW;

    @Column(name = "planned_visit_at")
    private Instant plannedVisitAt;

    @Column(name = "planned_slot_minutes")
    private Integer plannedSlotMinutes;

    @Enumerated(EnumType.STRING)
    @Column(name = "booking_channel", length = 20)
    private BookingChannel bookingChannel;

    @Column(name = "intake_notes")
    private String intakeNotes;

    @Builder.Default
    @Column(name = "requires_owner_approval_for_every_extra_work", nullable = false)
    private Boolean requiresOwnerApprovalForEveryExtraWork = false;

    @Builder.Default
    @Column(name = "planned_drop_off", nullable = false)
    private Boolean plannedDropOff = false;

    @Column(name = "checked_in_at")
    private Instant checkedInAt;

    @Column(name = "ready_for_owner_at")
    private Instant readyForOwnerAt;

    @Column(name = "handed_over_at")
    private Instant handedOverAt;

    @Column(name = "cancelled_at")
    private Instant cancelledAt;

    @Enumerated(EnumType.STRING)
    @Column(name = "cancellation_reason", length = 40)
    private CancellationReason cancellationReason;

    @Version
    @Builder.Default
    @Column(name = "version", nullable = false)
    private Long version = 0L;

    @Builder.Default
    @ColumnDefault("0")
    @Column(name = "labor_total", nullable = false, precision = 10, scale = 2)
    private BigDecimal laborTotal = BigDecimal.ZERO;

    @Builder.Default
    @ColumnDefault("0")
    @Column(name = "parts_total", nullable = false, precision = 10, scale = 2)
    private BigDecimal partsTotal = BigDecimal.ZERO;

    @Builder.Default
    @ColumnDefault("0")
    @Column(name = "costs_total", nullable = false, precision = 10, scale = 2)
    private BigDecimal costsTotal = BigDecimal.ZERO;

    @Builder.Default
    @ColumnDefault("0")
    @Column(name = "manual_discount_amount", nullable = false, precision = 10, scale = 2)
    private BigDecimal manualDiscountAmount = BigDecimal.ZERO;

    @Builder.Default
    @ColumnDefault("0")
    @Column(name = "points_discount_amount", nullable = false, precision = 10, scale = 2)
    private BigDecimal pointsDiscountAmount = BigDecimal.ZERO;

    @Builder.Default
    @ColumnDefault("0")
    @Column(name = "loyalty_points_spent", nullable = false)
    private Integer loyaltyPointsSpent = 0;

    @Builder.Default
    @ColumnDefault("0")
    @Column(name = "discount_amount", nullable = false, precision = 10, scale = 2)
    private BigDecimal discountAmount = BigDecimal.ZERO;

    @Builder.Default
    @ColumnDefault("0")
    @Column(name = "final_amount", nullable = false, precision = 10, scale = 2)
    private BigDecimal finalAmount = BigDecimal.ZERO;

    @ColumnDefault("CURRENT_TIMESTAMP")
    @Column(name = "created_at")
    private Instant createdAt;

    @ColumnDefault("CURRENT_TIMESTAMP")
    @Column(name = "updated_at")
    private Instant updatedAt;

    @Column(name = "completed_at")
    private Instant completedAt;

    @PrePersist
    private void prePersist() {
        Instant now = Instant.now();
        createdAt = now;
        updatedAt = now;
        if (status == null) {
            status = OrderStatus.NEW;
        }
        if (costsTotal == null) {
            costsTotal = BigDecimal.ZERO;
        }
        if (laborTotal == null) {
            laborTotal = BigDecimal.ZERO;
        }
        if (partsTotal == null) {
            partsTotal = BigDecimal.ZERO;
        }
        if (manualDiscountAmount == null) {
            manualDiscountAmount = BigDecimal.ZERO;
        }
        if (pointsDiscountAmount == null) {
            pointsDiscountAmount = BigDecimal.ZERO;
        }
        if (loyaltyPointsSpent == null) {
            loyaltyPointsSpent = 0;
        }
        if (discountAmount == null) {
            discountAmount = BigDecimal.ZERO;
        }
        if (finalAmount == null) {
            finalAmount = BigDecimal.ZERO;
        }
        if (requiresOwnerApprovalForEveryExtraWork == null) {
            requiresOwnerApprovalForEveryExtraWork = false;
        }
        if (plannedDropOff == null) {
            plannedDropOff = false;
        }
        if (version == null) {
            version = 0L;
        }
    }

    @PreUpdate
    private void preUpdate() {
        updatedAt = Instant.now();
    }
}
