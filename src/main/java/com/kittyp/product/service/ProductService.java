/**
 * @author rrohan419@gmail.com
 */
package com.kittyp.product.service;

import com.kittyp.common.model.PaginationModel;
import com.kittyp.product.dto.ProductFilterDto;
import com.kittyp.product.dto.ProductSaveDto;
import com.kittyp.product.model.ProductModel;

/**
 * @author rrohan419@gmail.com 
 */
public interface ProductService {

	ProductModel saveProduct(ProductSaveDto productSaveDto);
	
	ProductModel productByUuid(String uuid);
	
	PaginationModel<ProductModel> productsByFilter(ProductFilterDto productFilterDto, Integer pageNumber,
			Integer pageSize);
	
	Integer productCount(Boolean isActive);

	void updateProductStock(String productUuid, int quantityToReduce);

	void validateProductStock(String productUuid, int requestedQuantity);

	void reserveStock(String productUuid, int quantity, String orderNumber);

	void confirmStockReservation(String orderNumber);

	void cancelStockReservation(String orderNumber);
}
