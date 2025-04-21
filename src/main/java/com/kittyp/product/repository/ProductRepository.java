/**
 * @author rrohan419@gmail.com
 */
package com.kittyp.product.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import com.kittyp.product.entity.Product;

/**
 * @author rrohan419@gmail.com 
 */
public interface ProductRepository extends JpaRepository<Product, Long>, JpaSpecificationExecutor<Product>{

	Product findByUuid(String uuid);
	
	boolean existsByNameIgnoreCase(String name);
}
