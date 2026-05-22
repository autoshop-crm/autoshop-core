package com.vladko.autoshopcore.entities;

import com.vladko.autoshopcore.shared.entities.BaseEntity;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
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
public class Employee implements BaseEntity<Integer> {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Size(max = 50)
    @NotNull
    @Column(name = "first_name", nullable = false, length = 50)
    private String firstName;

    @Size(max = 50)
    @NotNull
    @Column(name = "last_name", nullable = false, length = 50)
    private String lastName;

    @Size(max = 100)
    @Column(name = "email", unique = true, length = 100)
    private String email;

    @NotNull
    @Column(name = "function", nullable = false, length = 20)
    @Enumerated(EnumType.STRING)
    private EmployeeType function;

    @ColumnDefault("CURRENT_TIMESTAMP")
    @Column(name = "created_at")
    private Instant createdAt;

    @PrePersist
    private void prePersist() {
        if (createdAt == null) {
            createdAt = Instant.now();
        }
    }
}
