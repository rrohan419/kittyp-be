package com.kittyp.clinic.entity;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

import com.kittyp.common.entity.BaseEntity;
import com.kittyp.common.entity.HasPublicId;
import com.kittyp.common.entity.PublicIdEntityListener;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.persistence.Index;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;
import lombok.EqualsAndHashCode;

@Entity
@Table(
    name = "clinic_inventory_batches",
    uniqueConstraints = {
        @UniqueConstraint(
            name = "uk_inventory_batch_clinic_item_number",
            columnNames = {"clinic_id", "item_id", "batch_number"}
        )
    },
    indexes = {
        @Index(
            name = "idx_inventory_batch_item_expiry",
            columnList = "clinic_id, item_id, expiry_date"
        )
    }
)
@EntityListeners(PublicIdEntityListener.class)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true, onlyExplicitlyIncluded = true)
@Builder
public class ClinicInventoryBatch extends BaseEntity implements HasPublicId {

    @EqualsAndHashCode.Include
    @Column(nullable = false, unique = true, updatable = false, length = 64)
    private String uuid;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "clinic_id", nullable = false)
    private Clinic clinic;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "item_id", nullable = false)
    private ClinicInventoryItem item;

    @Column(name = "batch_number", length = 100)
    private String batchNumber;

    @Column(name = "expiry_date")
    private LocalDate expiryDate;

    @Column(name = "non_expiring", nullable = false)
    @Builder.Default
    private Boolean nonExpiring = false;

    @Column(name = "quantity_received", nullable = false)
    private Integer quantityReceived;

    @Column(name = "quantity_on_hand", nullable = false)
    private Integer quantityOnHand;

    @Column(name = "unit_cost", nullable = false, precision = 12, scale = 2)
    private BigDecimal unitCost;

    /** True for migrated legacy stock whose expiry has not been reviewed; never billable. */
    @Column(name = "review_required", nullable = false, columnDefinition = "boolean not null default false")
    @Builder.Default
    private Boolean reviewRequired = false;

    @Column(name = "received_at", nullable = false)
    private LocalDateTime receivedAt;
}
