package com.vladko.autoshopcore.entities;

import com.vladko.autoshopcore.shared.entities.BaseEntity;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity(name = "loyalty_tiers")
public class LoyaltyTiers implements BaseEntity<Integer> {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "tierid", nullable = false)
    private Integer id;

    @Size(max = 30)
    @NotNull
    @Column(name = "name", nullable = false, length = 30)
    private String name;

    @NotNull
    @Column(name = "entry_spent_money", nullable = false, precision = 10, scale = 2)
    private BigDecimal entrySpentMoney;

    @NotNull
    @Column(name = "discount_percent", nullable = false)
    private Integer discountPercent;

    @NotNull
    @Column(name = "max_points_payment_percent", nullable = false)
    private Integer maxPointsPaymentPercent;


}
