package com.vladko.autoshopcore.entities;

import com.vladko.autoshopcore.client.entity.Customer;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.ColumnDefault;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity(name = "customer_order")
public class CustomerOrder {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "customer_id", nullable = false)
    private Customer customer;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "car_id", nullable = false)
    private Car car;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "employee_id", nullable = false)
    private Employee employee;

    @Column(name = "problem")
    private String problem;

    @ColumnDefault("NEW")
    @Enumerated(EnumType.STRING)
    private Status status;

    @ColumnDefault("0")
    @Column(name = "costs_total")
    private BigDecimal costsTotal;

    @ColumnDefault("0")
    @Column(name = "discount_amount")
    private BigDecimal discountAmount;

    @ColumnDefault("0")
    @Column(name = "final_amount")
    private BigDecimal final_amount;

    @ColumnDefault("CURRENT_TIMESTAMP")
    @Column(name = "created_at")
    LocalDate createdAt;

    @ColumnDefault("CURRENT_TIMESTAMP")
    @Column(name = "update_at")
    LocalDate updatedAt;

    @Column(name = "completed_at")
    LocalDate completedAt;
}
