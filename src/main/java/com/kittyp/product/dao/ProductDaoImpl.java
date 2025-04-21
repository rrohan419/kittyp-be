/**
 * @author rrohan419@gmail.com
 */
package com.kittyp.product.dao;

import org.springframework.core.env.Environment;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Repository;

import com.kittyp.common.constants.ExceptionConstant;
import com.kittyp.common.exception.CustomException;
import com.kittyp.product.entity.Product;
import com.kittyp.product.repository.ProductRepository;

import lombok.RequiredArgsConstructor;

/**
 * @author rrohan419@gmail.com 
 */
@Repository
@RequiredArgsConstructor
public class ProductDaoImpl implements ProductDao {

	private final ProductRepository productRepository;
	private final Environment env;
	
	/**
	 * @author rrohan419@gmail.com
	 */
	@Override
	public Product saveProduct(Product product) {
		try {
			return productRepository.save(product);
		} catch (Exception e) {
			throw new CustomException(env.getProperty(ExceptionConstant.ERROR_DATABASE_OPERATION),
					HttpStatus.INTERNAL_SERVER_ERROR, e);

		}
	}

	/**
	 * @author rrohan419@gmail.com
	 */
	@Override
	public Page<Product> productsByFilter(Pageable pageable, Specification<Product> specification) {
		try {
			return productRepository.findAll(specification, pageable);
		} catch (Exception e) {
			throw new CustomException(env.getProperty(ExceptionConstant.ERROR_DATABASE_OPERATION),
					HttpStatus.INTERNAL_SERVER_ERROR, e);
		}
	}

	/**
	 * @author rrohan419@gmail.com
	 */
	@Override
	public Product productUuid(String uuid) {
		try {
			return productRepository.findByUuid(uuid);
		} catch (Exception e) {
			throw new CustomException(env.getProperty(ExceptionConstant.ERROR_DATABASE_OPERATION),
					HttpStatus.INTERNAL_SERVER_ERROR, e);

		}
	}

	/**
	 * @author rrohan419@gmail.com
	 */
	@Override
	public boolean productExistsByName(String name) {
		try {
			return productRepository.existsByNameIgnoreCase(name);
		} catch (Exception e) {
			throw new CustomException(env.getProperty(ExceptionConstant.ERROR_DATABASE_OPERATION),
					HttpStatus.INTERNAL_SERVER_ERROR, e);

		}
	}

}
