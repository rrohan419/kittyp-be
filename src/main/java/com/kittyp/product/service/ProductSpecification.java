/**
 * @author rrohan419@gmail.com
 */
package com.kittyp.product.service;

import java.util.ArrayList;
import java.util.List;

import org.springframework.data.jpa.domain.Specification;

import com.kittyp.common.constants.KeyConstant;
import com.kittyp.product.dto.ProductFilterDto;
import com.kittyp.product.entity.Product;

import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;

/**
 * @author rrohan419@gmail.com 
 */
public class ProductSpecification {

	private ProductSpecification() {}
	
	public static Specification<Product> productByFilters(ProductFilterDto productFilterDto) {
		return (Root<Product> root, CriteriaQuery<?> query, CriteriaBuilder builder) -> {
			List<Predicate> predicates = new ArrayList<>();
			
			predicates.add(builder.equal(root.get(KeyConstant.IS_ACTIVE), true));
			
			if(productFilterDto.getName() != null && !productFilterDto.getName().isEmpty()) {
				predicates.add(builder.like(builder.lower(root.get(KeyConstant.PRODUCT_NAME)), "%" + productFilterDto.getName().toLowerCase() + "%"));
			}
			
			if(productFilterDto.getCategory() != null && !productFilterDto.getCategory().isEmpty()) {
				predicates.add(builder.equal(builder.lower(root.get(KeyConstant.PRODUCT_CATEGORY)), productFilterDto.getCategory()));
			}
			
			if(productFilterDto.getMinPrice() != null) {
				predicates.add(builder.greaterThanOrEqualTo(root.get(KeyConstant.PRODUCT_PRICE), productFilterDto.getMinPrice()));
			}
			
			if(productFilterDto.getMaxPrice() != null) {
				predicates.add(builder.lessThanOrEqualTo(root.get(KeyConstant.PRODUCT_PRICE), productFilterDto.getMaxPrice()));
			}
			
			return builder.and(predicates.toArray(new Predicate[0]));
		};
	}
}
