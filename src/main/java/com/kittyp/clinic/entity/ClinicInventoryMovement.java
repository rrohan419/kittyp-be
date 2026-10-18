package com.kittyp.clinic.entity;

import com.kittyp.clinic.enums.InventoryMovementAction;
import com.kittyp.clinic.enums.InventoryMovementSource;
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
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "clinic_inventory_movements")
@EntityListeners(PublicIdEntityListener.class)
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(callSuper = true)
public class ClinicInventoryMovement extends BaseEntity implements HasPublicId {

    @Column(nullable = false, unique = true, updatable = false, length = 64)
    private String uuid;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "clinic_id", nullable = false)
    private Clinic clinic;

    @Column(name = "item_uuid", nullable = false, length = 64)
    private String itemUuid;

    @Column(name = "item_name", nullable = false, length = 255)
    private String itemName;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "actor_id")
    private User actor;

    @Column(name = "actor_name", length = 255)
    private String actorName;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private InventoryMovementAction action;

    @Column(name = "quantity_delta", nullable = false)
    private Integer quantityDelta;

    @Column(name = "quantity_after", nullable = false)
    private Integer quantityAfter;

    @Column(length = 500)
    private String note;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private InventoryMovementSource source;

    @Column(name = "invoice_uuid", length = 64)
    private String invoiceUuid;

    @Column(name = "invoice_number", length = 64)
    private String invoiceNumber;
}
