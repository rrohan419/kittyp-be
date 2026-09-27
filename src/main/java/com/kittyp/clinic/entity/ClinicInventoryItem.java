package com.kittyp.clinic.entity;

import java.math.BigDecimal;

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
@Table(name = "clinic_inventory_items")
@EntityListeners(PublicIdEntityListener.class)
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@DynamicUpdate
@EqualsAndHashCode(callSuper = true)
public class ClinicInventoryItem extends BaseEntity implements HasPublicId {

    @Column(nullable = false, unique = true, updatable = false, length = 12)
    private String uuid;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "clinic_id", nullable = false)
    private Clinic clinic;

    @Column(nullable = false, length = 200)
    private String name;

    /** medication | supply | equipment | food */
    @Column(nullable = false, length = 32)
    private String category;

    /** On-hand total across active lots (denormalized). */
    @Column(nullable = false, precision = 19, scale = 3)
    private BigDecimal stock;

    @Column(nullable = false, length = 40, columnDefinition = "varchar(40) default 'pcs'")
    @Builder.Default
    private String unit = "pcs";

    @Column(name = "min_stock", nullable = false, columnDefinition = "integer default 0")
    @Builder.Default
    private Integer minStock = 0;

    @Column(length = 64)
    private String barcode;

    @Column(length = 64)
    private String gtin;

    @Column(length = 64)
    private String sku;

    @Column(length = 200)
    private String manufacturer;

    @Column(name = "track_stock", nullable = false, columnDefinition = "boolean default true")
    @Builder.Default
    private Boolean trackStock = true;

    /** Sell / bill price. */
    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal price;

    /** Optional cost for stock-value analytics. */
    @Column(name = "purchase_price", precision = 19, scale = 2)
    private BigDecimal purchasePrice;
}
