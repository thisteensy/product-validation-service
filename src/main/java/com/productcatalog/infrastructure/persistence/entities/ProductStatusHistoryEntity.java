package com.productcatalog.infrastructure.persistence.entities;

import com.productcatalog.domain.model.ChangedByType;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "product_status_history")
public class ProductStatusHistoryEntity {

    @Id
    @Column(name = "id", length = 36)
    private String id;

    @Column(name = "product_id", length = 36, nullable = false)
    private String productId;

    @Column(name = "previous_status", nullable = false)
    private String previousStatus;

    @Column(name = "new_status", nullable = false)
    private String newStatus;

    @Enumerated(EnumType.STRING)
    @Column(name = "changed_by_type", nullable = false)
    private ChangedByType changedByType;

    @Column(name = "changed_by_id")
    private String changedById;

    @Column(name = "notes", columnDefinition = "TEXT")
    private String notes;

    @Column(name = "changed_at", nullable = false)
    private LocalDateTime changedAt;
}