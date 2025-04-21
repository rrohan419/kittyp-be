/**
 * @author rrohan419@gmail.com
 */
package com.kittyp.product.dao;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

import com.kittyp.product.entity.Product;

/**
 * @author rrohan419@gmail.com 
 */
public interface ProductDao {

	Product saveProduct(Product product);
	
	Page<Product> productsByFilter(Pageable pageable, Specification<Product> specification);

	Product productUuid(String uuid);
	
	boolean productExistsByName(String name);
}
