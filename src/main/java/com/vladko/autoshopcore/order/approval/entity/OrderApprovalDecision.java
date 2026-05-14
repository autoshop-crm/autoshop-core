package com.vladko.autoshopcore.order.approval.entity;

import com.vladko.autoshopcore.shared.entities.BaseEntity;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.ColumnDefault;

import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "order_approval_decision")
public class OrderApprovalDecision implements BaseEntity<Integer> {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "approval_request_id", nullable = false)
    private OrderApprovalRequest approvalRequest;

    @Enumerated(EnumType.STRING)
    @Column(name = "decision", nullable = false, length = 32)
    private OrderApprovalDecisionType decision;

    @Column(name = "decision_token", nullable = false, length = 120)
    private String decisionToken;

    @Column(name = "idempotency_key", nullable = false, unique = true, length = 255)
    private String idempotencyKey;

    @Column(name = "comment", length = 1000)
    private String comment;

    @Column(name = "decision_at", nullable = false)
    private Instant decisionAt;

    @ColumnDefault("CURRENT_TIMESTAMP")
    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @PrePersist
    void prePersist() {
        Instant now = Instant.now();
        if (decisionAt == null) {
            decisionAt = now;
        }
        if (createdAt == null) {
            createdAt = now;
        }
    }
}
