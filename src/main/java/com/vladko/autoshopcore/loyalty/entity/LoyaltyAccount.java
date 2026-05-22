package com.vladko.autoshopcore.loyalty.entity;

import com.vladko.autoshopcore.client.entity.Customer;
import com.vladko.autoshopcore.shared.entities.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "loyalty_account")
public class LoyaltyAccount implements BaseEntity<Integer> {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "customer_id", nullable = false, unique = true)
    private Customer customer;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "tier_id", nullable = false)
    private LoyaltyTier tier;

    @Builder.Default
    @Column(name = "balance", nullable = false)
    private Integer balance = 0;

    @Builder.Default
    @Column(name = "total_spent", nullable = false, precision = 10, scale = 2)
    private BigDecimal totalSpent = BigDecimal.ZERO;

    @Builder.Default
    @Column(name = "total_earned_points", nullable = false)
    private Integer totalEarnedPoints = 0;

    @Column(name = "created_at")
    private Instant createdAt;

    @Column(name = "updated_at")
    private Instant updatedAt;

    @PrePersist
    private void prePersist() {
        Instant now = Instant.now();
        createdAt = now;
        updatedAt = now;
        if (balance == null) {
            balance = 0;
        }
        if (totalSpent == null) {
            totalSpent = BigDecimal.ZERO;
        }
        if (totalEarnedPoints == null) {
            totalEarnedPoints = 0;
        }
    }

    @PreUpdate
    private void preUpdate() {
        updatedAt = Instant.now();
    }
}
