/**
 * @author rrohan419@gmail.com
 */
package com.kittyp.payment.entity;

import java.math.BigDecimal;

import com.kittyp.common.entity.BaseEntity;
import com.kittyp.payment.enums.ChargeType;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "charges")
@Data
@EqualsAndHashCode(callSuper = false)
@NoArgsConstructor
@AllArgsConstructor
public class ChargeConfig extends BaseEntity {

	private static final long serialVersionUID = 1L;

	@Enumerated(EnumType.STRING)
    @Column(nullable = false, unique = true)
    private ChargeType type;

    @Column(nullable = false, precision = 5, scale = 4) // e.g. 0.1800 = 18%
    private BigDecimal rate;

    @Column(nullable = false)
    private boolean isPercentage;

    @Column
    private String description;
}

