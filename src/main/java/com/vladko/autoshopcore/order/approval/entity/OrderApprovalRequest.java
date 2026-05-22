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
@Table(name = "order_approval_request")
public class OrderApprovalRequest implements BaseEntity<Integer> {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "order_id", nullable = false)
    private Order order;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "proposal_id", nullable = false, unique = true)
    private OrderWorkProposal proposal;

    @Enumerated(EnumType.STRING)
    @Column(name = "approval_type", nullable = false, length = 32)
    private OrderApprovalType approvalType;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 32)
    private OrderApprovalRequestStatus status;

    @Column(name = "request_token", nullable = false, unique = true, length = 120)
    private String requestToken;

    @Column(name = "requested_amount", nullable = false, precision = 10, scale = 2)
    private BigDecimal requestedAmount;

    @Column(name = "requested_at", nullable = false)
    private Instant requestedAt;

    @Column(name = "expires_at")
    private Instant expiresAt;

    @Column(name = "customer_contact_channel", length = 50)
    private String customerContactChannel;

    @Builder.Default
    @Column(name = "decision_version", nullable = false)
    private Long decisionVersion = 0L;

    @ColumnDefault("CURRENT_TIMESTAMP")
    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @PrePersist
    void prePersist() {
        Instant now = Instant.now();
        if (requestedAt == null) {
            requestedAt = now;
        }
        if (createdAt == null) {
            createdAt = now;
        }
        if (decisionVersion == null) {
            decisionVersion = 0L;
        }
    }
}
