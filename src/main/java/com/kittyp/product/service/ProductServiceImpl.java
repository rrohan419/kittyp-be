/**
 * @author rrohan419@gmail.com
 */
package com.kittyp.product.service;

import java.util.List;
import java.util.UUID;

import org.springframework.core.env.Environment;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import com.kittyp.common.exception.CustomException;
import com.kittyp.common.model.PaginationModel;
import com.kittyp.common.util.Mapper;
import com.kittyp.product.dao.ProductDao;
import com.kittyp.product.dto.ProductFilterDto;
import com.kittyp.product.dto.ProductSaveDto;
import com.kittyp.product.entity.Product;
import com.kittyp.product.model.ProductModel;

import lombok.RequiredArgsConstructor;

/**
 * @author rrohan419@gmail.com 
 */
@Service
@RequiredArgsConstructor
public class ProductServiceImpl implements ProductService {

	private final ProductDao productDao;
	private final Environment env;
	private final Mapper mapper;
	
	/**
	 * @author rrohan419@gmail.com
	 */
	@Override
	public ProductModel saveProduct(ProductSaveDto productSaveDto) {
		if(productDao.productExistsByName(productSaveDto.getName())) {
			throw new CustomException("product exists with name : "+productSaveDto.getName(), HttpStatus.CONFLICT);
		}
		
		Product product = mapper.convert(productSaveDto, Product.class);
		product.setUuid(UUID.randomUUID().toString());
		product = productDao.saveProduct(product);
		
		return mapper.convert(product, ProductModel.class);
	}

	/**
	 * @author rrohan419@gmail.com
	 */
	@Override
	public ProductModel productByUuid(String uuid) {
		Product product = productDao.productUuid(uuid);
		
		if(product == null) {
			throw new CustomException("product not found by uuid : "+uuid, HttpStatus.NOT_FOUND);
		}
		return mapper.convert(product, ProductModel.class);
	}

	/**
	 * @author rrohan419@gmail.com
	 */
	@Override
	public PaginationModel<ProductModel> productsByFilter(ProductFilterDto productFilterDto, Integer pageNumber,
			Integer pageSize) {
		Pageable pageable = PageRequest.of(pageNumber-1, pageSize);
		
		Specification<Product> productSpecification = ProductSpecification.productByFilters(productFilterDto);
		
		Page<Product> productPage = productDao.productsByFilter(pageable, productSpecification);
		
		List<ProductModel> productListModel = productPage.getContent().stream().map(product -> mapper.convert(product, ProductModel.class)).toList();
	
		return productPageToModel(new PageImpl<>(productListModel, pageable, productPage.getTotalElements()));
	}
	
	private PaginationModel<ProductModel> productPageToModel(Page<ProductModel> productPage) {
		PaginationModel<ProductModel> projectModel = new PaginationModel<>();
		projectModel.setModels(productPage.getContent());
		projectModel.setIsFirst(productPage.isFirst());
		projectModel.setIsLast(productPage.isLast());
		projectModel.setTotalElements(productPage.getTotalElements());
		projectModel.setTotalPages(productPage.getTotalPages());
		return projectModel;
	}

	/**
	 * @author rrohan419@gmail.com
	 */
	@Override
	public Integer productCount(Boolean isActive) {
		return productDao.productCount(isActive == null ? true : isActive);
	}

}
