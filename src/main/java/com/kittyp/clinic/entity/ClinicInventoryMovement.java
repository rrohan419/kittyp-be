package com.kittyp.clinic.entity;

import java.math.BigDecimal;

import org.hibernate.annotations.DynamicUpdate;

import com.kittyp.clinic.enums.InventoryMovementType;
import com.kittyp.common.entity.BaseEntity;
import com.kittyp.common.entity.HasPublicId;
import com.kittyp.common.entity.PublicIdEntityListener;
import com.kittyp.user.entity.User;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@Entity
@Table(
        name = "clinic_inventory_movements",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_inv_mov_invoice_line",
                columnNames = { "invoice_uuid", "inventory_item_id", "invoice_line_key" }))
@EntityListeners(PublicIdEntityListener.class)
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@DynamicUpdate
@EqualsAndHashCode(callSuper = true)
public class ClinicInventoryMovement extends BaseEntity implements HasPublicId {

    @Column(nullable = false, unique = true, updatable = false, length = 12)
    private String uuid;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "clinic_id", nullable = false)
    private Clinic clinic;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "inventory_item_id", nullable = false)
    private ClinicInventoryItem inventoryItem;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "lot_id")
    private ClinicInventoryLot lot;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private InventoryMovementType type;

    @Column(nullable = false, precision = 19, scale = 3)
    private BigDecimal quantity;

    @Column(name = "previous_qty", nullable = false, precision = 19, scale = 3)
    private BigDecimal previousQty;

    @Column(name = "new_qty", nullable = false, precision = 19, scale = 3)
    private BigDecimal newQty;

    @Column(name = "invoice_uuid", length = 12)
    private String invoiceUuid;

    @Column(name = "invoice_line_key", length = 64)
    private String invoiceLineKey;

    @Column(length = 500)
    private String notes;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "actor_user_id")
    private User actor;
}
