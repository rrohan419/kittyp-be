package com.kittyp.clinic.entity;

import java.math.BigDecimal;
import java.time.LocalDate;

import org.hibernate.annotations.DynamicUpdate;

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
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "clinic_inventory_lots")
@EntityListeners(PublicIdEntityListener.class)
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@DynamicUpdate
@EqualsAndHashCode(callSuper = true)
public class ClinicInventoryLot extends BaseEntity implements HasPublicId {

    @Column(nullable = false, unique = true, updatable = false, length = 12)
    private String uuid;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "clinic_id", nullable = false)
    private Clinic clinic;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "inventory_item_id", nullable = false)
    private ClinicInventoryItem inventoryItem;

    @Column(name = "lot_number", length = 80)
    private String lotNumber;

    @Column(length = 200)
    private String manufacturer;

    @Column(name = "manufactured_on")
    private LocalDate manufacturedOn;

    @Column(name = "expires_on")
    private LocalDate expiresOn;

    @Column(nullable = false, precision = 19, scale = 3)
    private BigDecimal quantity;
}
