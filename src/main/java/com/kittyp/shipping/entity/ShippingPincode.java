package com.kittyp.shipping.entity;

import java.math.BigDecimal;

import org.hibernate.annotations.DynamicUpdate;

import com.kittyp.common.entity.BaseEntity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "shipping_pincodes")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@DynamicUpdate
@EqualsAndHashCode(callSuper = true)
public class ShippingPincode extends BaseEntity {

    @Column(nullable = false, unique = true, length = 10)
    private String pincode;

    @Column(nullable = false)
    private Boolean serviceable;

    private Integer estimatedDays;

    @Column(nullable = false)
    private Boolean codAvailable;

    private BigDecimal shippingCost;

    @Column(length = 255)
    private String productUuid;

    @Column(length = 1024)
    private String notes;
}
