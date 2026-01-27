package com.vladko.autoshopcore.entities;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.ColumnDefault;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

import java.time.Instant;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
public class LoyaltyTransactions {
    @Size(max = 30)
    @NotNull
    @Column(name = "operation_type", nullable = false, length = 30)
    @Enumerated(EnumType.STRING)
    private OperationType operationType;

    @NotNull
    @Column(name = "count_scores", nullable = false)
    private Integer countScores;

    @ColumnDefault("CURRENT_TIMESTAMP")
    @Column(name = "date_transaction")
    private Instant dateTransaction;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id")
    private CustomerOrder order;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    @JoinColumn(name = "account_id", nullable = false)
    private LoyaltyAccount account;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "transactionid", nullable = false)
    private Integer id;
}
