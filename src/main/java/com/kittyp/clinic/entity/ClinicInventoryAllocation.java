
package com.kittyp.clinic.entity;

import java.math.BigDecimal;

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
import jakarta.persistence.Index;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;
import lombok.EqualsAndHashCode;

@Entity
@Table(
    name = "clinic_inventory_allocations",
    indexes = {
        @Index(
            name = "idx_inventory_allocation_invoice",
            columnList = "invoice_uuid, invoice_line_index"
        ),
        @Index(
            name = "idx_inventory_allocation_batch",
            columnList = "batch_id"
        )
    }
)
@EntityListeners(PublicIdEntityListener.class)
@Getter
@Setter
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true, onlyExplicitlyIncluded = true)
public class ClinicInventoryAllocation extends BaseEntity
        implements HasPublicId {

    @EqualsAndHashCode.Include
    @Column(nullable = false, unique = true, updatable = false, length = 64)
    private String uuid;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "clinic_id", nullable = false)
    private Clinic clinic;

    @Column(name = "invoice_uuid", nullable = false, length = 64)
    private String invoiceUuid;

    // Index of the line in the current JSON invoice representation.
    // Prefer a stable invoice-line UUID when your schema supports it.
    @Column(name = "invoice_line_index", nullable = false)
    private Integer invoiceLineIndex;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "item_id", nullable = false)
    private ClinicInventoryItem item;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "batch_id", nullable = false)
    private ClinicInventoryBatch batch;

    @Column(nullable = false)
    private Integer quantity;

    @Column(name = "unit_cost", nullable = false, precision = 12, scale = 2)
    private BigDecimal unitCost;
}
