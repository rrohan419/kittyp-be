/**
 * @author rrohan419@gmail.com
 */
package com.kittyp.product.dto;

import java.math.BigDecimal;
import java.util.Set;

import com.kittyp.order.emus.CurrencyType;
import com.kittyp.product.entity.ProductAttributes;
import com.kittyp.product.enums.ProductStatus;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;

/**
 * @author rrohan419@gmail.com 
 */
@Getter
public class ProductSaveDto {

	@NotBlank
	private String name;
	
	@NotBlank
	private String description;
	
	@NotNull
	private BigDecimal price;
	
	@NotNull
	private CurrencyType currency;
	
	@NotNull
	private ProductStatus status;
	
	@NotNull
	private Set<String> productImageUrls;
	
	@NotNull
	private Integer stockQuantity;
	
	@NotBlank
	private String sku;
	
	@NotBlank
	private String category;
	
	private ProductAttributes attributes;
}
