package com.vladko.autoshopcore.loyalty.entity;

import com.vladko.autoshopcore.shared.entities.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "loyalty_tier")
public class LoyaltyTier implements BaseEntity<Integer> {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "name", nullable = false, unique = true, length = 30)
    private String name;

    @Column(name = "entry_spent_money", nullable = false, precision = 10, scale = 2)
    private BigDecimal entrySpentMoney;

    @Column(name = "discount_percent", nullable = false)
    private Integer discountPercent;

    @Column(name = "max_points_payment_percent", nullable = false)
    private Integer maxPointsPaymentPercent;
}
