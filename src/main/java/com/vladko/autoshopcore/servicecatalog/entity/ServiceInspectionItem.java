package com.vladko.autoshopcore.servicecatalog.entity;

import com.vladko.autoshopcore.entities.Services;
import com.vladko.autoshopcore.shared.entities.BaseEntity;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.ColumnDefault;

import java.time.Instant;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Entity
@Table(name = "service_inspection_item")
public class ServiceInspectionItem implements BaseEntity<Integer> {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "service_id", nullable = false)
    private Services service;

    @Column(name = "title", nullable = false, length = 255)
    private String title;

    @Builder.Default
    @Column(name = "active", nullable = false)
    private Boolean active = true;

    @ColumnDefault("CURRENT_TIMESTAMP")
    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @PrePersist
    void prePersist() {
        if (createdAt == null) {
            createdAt = Instant.now();
        }
        if (active == null) {
            active = true;
        }
    }
}
