/**
 * @author rrohan419@gmail.com
 */
package com.kittyp.product.entity;

import java.math.BigDecimal;
import java.util.Set;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.Type;
import org.hibernate.type.SqlTypes;

import com.kittyp.common.entity.BaseEntity;
import com.kittyp.order.emus.CurrencyType;
import com.kittyp.product.enums.ProductStatus;
import com.vladmihalcea.hibernate.type.json.JsonBinaryType;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

/**
 * @author rrohan419@gmail.com 
 */
@Entity
@Table(name = "products")
@Data
@EqualsAndHashCode(callSuper=false)
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Product extends BaseEntity {
	
	private static final long serialVersionUID = 1L;

	@Column(nullable = false, unique = true)
    private String uuid;
	
	@Column(name = "name", nullable = false, length = 255)
    private String name;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "price", nullable = false, precision = 10, scale = 2)
    private BigDecimal price;
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10, columnDefinition = "VARCHAR(10) DEFAULT 'INR'")
	@Builder.Default
	private CurrencyType currency = CurrencyType.INR;
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ProductStatus status;
    
    @Column(columnDefinition = "jsonb")
    @Type(JsonBinaryType.class)
    private Set<String> productImageUrls;

    @Column(name = "stock_quantity", nullable = false)
    private Integer stockQuantity;
    
    @Column(name = "sku", unique = true, nullable = false, length = 50)
    private String sku;
    
    @Column(name = "category", nullable = false, length = 100)
    private String category;
    
    @Column(name = "attributes", columnDefinition = "json")
    @JdbcTypeCode(SqlTypes.JSON)
    private ProductAttributes attributes;
}
