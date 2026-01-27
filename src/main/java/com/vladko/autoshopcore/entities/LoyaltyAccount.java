package com.vladko.autoshopcore.entities;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.ColumnDefault;

import java.math.BigDecimal;
import java.time.Instant;

@Getter
@Setter
@Entity
@Table(name = "loyalty_accounts")
public class LoyaltyAccount {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "accountid", nullable = false)
    private Integer id;

    @NotNull
    @ColumnDefault("0")
    @Column(name = "balance", nullable = false)
    private Integer balance;

    @NotNull
    @ColumnDefault("0")
    @Column(name = "total_spent", nullable = false, precision = 10, scale = 2)
    private BigDecimal totalSpent;

    @NotNull
    @ColumnDefault("0")
    @Column(name = "total_scores", nullable = false)
    private Integer totalScores;

    @ColumnDefault("CURRENT_TIMESTAMP")
    @Column(name = "created_at")
    private Instant createdAt;

    @ColumnDefault("CURRENT_TIMESTAMP")
    @Column(name = "updated_at")
    private Instant updatedAt;


}