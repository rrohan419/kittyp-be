package com.kittyp.clinic.entity;

import java.time.LocalDateTime;

import org.hibernate.annotations.DynamicUpdate;

import com.kittyp.clinic.enums.InventoryAlertType;
import com.kittyp.common.entity.BaseEntity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
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
        name = "clinic_inventory_alert_states",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_inv_alert_state",
                columnNames = { "clinic_id", "alert_type", "inventory_item_id", "state_key" }))
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@DynamicUpdate
@EqualsAndHashCode(callSuper = true)
public class ClinicInventoryAlertState extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "clinic_id", nullable = false)
    private Clinic clinic;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "inventory_item_id")
    private ClinicInventoryItem inventoryItem;

    @Enumerated(EnumType.STRING)
    @Column(name = "alert_type", nullable = false, length = 32)
    private InventoryAlertType alertType;

    @Column(name = "state_key", nullable = false, length = 120)
    private String stateKey;

    @Column(name = "message", length = 500)
    private String message;

    @Column(name = "last_notified_at")
    private LocalDateTime lastNotifiedAt;

    @Column(name = "resolved_at")
    private LocalDateTime resolvedAt;
}
