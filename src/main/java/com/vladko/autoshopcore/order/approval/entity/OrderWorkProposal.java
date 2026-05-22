package com.vladko.autoshopcore.order.approval.entity;

import com.vladko.autoshopcore.order.entity.Order;
import com.vladko.autoshopcore.shared.entities.BaseEntity;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.ColumnDefault;

import java.math.BigDecimal;
import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "order_work_proposal")
public class OrderWorkProposal implements BaseEntity<Integer> {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "order_id", nullable = false)
    private Order order;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 32)
    private OrderWorkProposalStatus status;

    @Enumerated(EnumType.STRING)
    @Column(name = "approval_type", nullable = false, length = 32)
    private OrderApprovalType approvalType;

    @Column(name = "title", nullable = false, length = 255)
    private String title;

    @Column(name = "description", length = 1000)
    private String description;

    @Column(name = "labor_amount", nullable = false, precision = 10, scale = 2)
    private BigDecimal laborAmount;

    @Column(name = "parts_amount", nullable = false, precision = 10, scale = 2)
    private BigDecimal partsAmount;

    @Column(name = "total_amount", nullable = false, precision = 10, scale = 2)
    private BigDecimal totalAmount;

    @Builder.Default
    @Column(name = "requires_every_extra_work_approval", nullable = false)
    private Boolean requiresEveryExtraWorkApproval = false;

    @ColumnDefault("CURRENT_TIMESTAMP")
    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @PrePersist
    void prePersist() {
        if (createdAt == null) {
            createdAt = Instant.now();
        }
    }
}
