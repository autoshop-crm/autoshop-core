package com.vladko.autoshopcore.order.timeline.entity;

import com.vladko.autoshopcore.order.entity.Order;
import com.vladko.autoshopcore.order.entity.OrderStatus;
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
@Table(name = "order_timeline_entry")
public class OrderTimelineEntry implements BaseEntity<Integer> {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "order_id", nullable = false)
    private Order order;

    @Enumerated(EnumType.STRING)
    @Column(name = "event_type", nullable = false, length = 64)
    private OrderTimelineEventType eventType;

    @Enumerated(EnumType.STRING)
    @Column(name = "visibility", nullable = false, length = 32)
    private OrderTimelineVisibility visibility;

    @Enumerated(EnumType.STRING)
    @Column(name = "actor_type", nullable = false, length = 32)
    private OrderTimelineActorType actorType;

    @Column(name = "actor_id")
    private Long actorId;

    @Enumerated(EnumType.STRING)
    @Column(name = "effective_status", length = 40)
    private OrderStatus effectiveStatus;

    @Column(name = "summary", nullable = false, length = 255)
    private String summary;

    @Column(name = "details_json", length = 2000)
    private String detailsJson;

    @Column(name = "dedupe_key", nullable = false, length = 255)
    private String dedupeKey;

    @Column(name = "occurred_at", nullable = false)
    private Instant occurredAt;

    @ColumnDefault("CURRENT_TIMESTAMP")
    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @PrePersist
    void prePersist() {
        Instant now = Instant.now();
        if (occurredAt == null) occurredAt = now;
        if (createdAt == null) createdAt = now;
    }
}
