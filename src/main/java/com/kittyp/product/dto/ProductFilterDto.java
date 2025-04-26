/**
 * @author rrohan419@gmail.com
 */
package com.kittyp.product.dto;

import com.kittyp.product.enums.ProductStatus;

import lombok.Getter;

/**
 * @author rrohan419@gmail.com 
 */
@Getter
public class ProductFilterDto {

	private String name;
	
	private String category;
	
	private Integer minPrice;
	
	private Integer maxPrice;
	
	private ProductStatus status;
	
	private Boolean isRandom;
}
