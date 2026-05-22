package com.vladko.autoshopcore.parts.entity;

import com.vladko.autoshopcore.order.approval.entity.OrderApprovalRequest;
import com.vladko.autoshopcore.order.entity.Order;
import com.vladko.autoshopcore.shared.entities.BaseEntity;
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
@Entity
@Table(name = "order_requested_part")
public class OrderRequestedPart implements BaseEntity<Integer> {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id", nullable = false)
    private Order order;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "approval_request_id")
    private OrderApprovalRequest approvalRequest;

    @Column(name = "article_number", nullable = false, length = 30)
    private String articleNumber;

    @Column(name = "brand", length = 20)
    private String brand;

    @Column(name = "name", nullable = false, length = 100)
    private String name;

    @Column(name = "umapi_article_id")
    private Integer umapiArticleId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "matched_local_part_id")
    private Part matchedLocalPart;

    @Column(name = "requested_quantity", nullable = false)
    private Integer requestedQuantity;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 32)
    private OrderRequestedPartStatus status;

    @Column(name = "selected_supplier", length = 50)
    private String selectedSupplier;

    @Column(name = "selected_quote_signature", length = 255)
    private String selectedQuoteSignature;

    @Column(name = "purchase_price", precision = 10, scale = 2)
    private BigDecimal purchasePrice;

    @Column(name = "sale_price", precision = 10, scale = 2)
    private BigDecimal salePrice;

    @Column(name = "currency", length = 10)
    private String currency;

    @Column(name = "delivery_days_min")
    private Integer deliveryDaysMin;

    @Column(name = "delivery_days_max")
    private Integer deliveryDaysMax;

    @Column(name = "quote_fetched_at")
    private Instant quoteFetchedAt;

    @Column(name = "ordered_at")
    private Instant orderedAt;

    @Column(name = "received_at")
    private Instant receivedAt;

    @ColumnDefault("CURRENT_TIMESTAMP")
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @ColumnDefault("CURRENT_TIMESTAMP")
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @PrePersist
    private void prePersist() {
        Instant now = Instant.now();
        if (createdAt == null) {
            createdAt = now;
        }
        updatedAt = now;
    }

    @PreUpdate
    private void preUpdate() {
        updatedAt = Instant.now();
    }
}
