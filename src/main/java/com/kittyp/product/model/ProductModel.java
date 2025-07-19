/**
 * @author rrohan419@gmail.com
 */
package com.kittyp.product.model;

import java.math.BigDecimal;
import java.util.Set;

import com.kittyp.order.emus.CurrencyType;
import com.kittyp.product.entity.ProductAttributes;
import com.kittyp.product.enums.ProductStatus;

import lombok.Data;

/**
 * @author rrohan419@gmail.com 
 */
@Data
public class ProductModel {

	private String uuid;
	private String name;
	private String description;
	private BigDecimal price;
	private CurrencyType currency;
	private ProductStatus status;
	private Set<String> productImageUrls;
	private String category;
	private ProductAttributes attributes;
	private Integer stockQuantity;
	private String sku;
}
