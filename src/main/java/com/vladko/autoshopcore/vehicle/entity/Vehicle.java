package com.vladko.autoshopcore.vehicle.entity;

import com.vladko.autoshopcore.client.entity.Customer;
import com.vladko.autoshopcore.shared.entities.BaseEntity;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.ColumnDefault;

import java.time.Instant;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "vehicle")
public class Vehicle implements BaseEntity<Integer> {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "customer_id", nullable = false)
    private Customer customer;

    @Column(name = "brand", nullable = false, length = 25)
    private String brand;

    @Column(name = "model", nullable = false, length = 25)
    private String model;

    @Column(name = "vin", unique = true, nullable = false, length = 17)
    private String vin;

    @Column(name = "license_plate", unique = true, nullable = false, length = 12)
    private String licensePlate;

    @Column(name = "umapi_type", length = 20)
    private String umapiType;

    @Column(name = "umapi_manufacturer_id")
    private Integer umapiManufacturerId;

    @Column(name = "umapi_manufacturer_name", length = 100)
    private String umapiManufacturerName;

    @Column(name = "umapi_model_series_id")
    private Integer umapiModelSeriesId;

    @Column(name = "umapi_model_series_name", length = 150)
    private String umapiModelSeriesName;

    @Column(name = "umapi_modification_id")
    private Integer umapiModificationId;

    @Column(name = "umapi_modification_name")
    private String umapiModificationName;

    @Column(name = "umapi_engine_description")
    private String umapiEngineDescription;

    @Column(name = "umapi_catalog_linked_at")
    private Instant umapiCatalogLinkedAt;

    @ColumnDefault("CURRENT_TIMESTAMP")
    @Column(name = "created_at")
    private Instant createdAt;

    @ColumnDefault("CURRENT_TIMESTAMP")
    @Column(name = "updated_at")
    private Instant updatedAt;

    @PrePersist
    private void prePersist() {
        Instant now = Instant.now();
        createdAt = now;
        updatedAt = now;
    }

    @PreUpdate
    private void preUpdate() {
        updatedAt = Instant.now();
    }
}
