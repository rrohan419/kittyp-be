/**
 * @author rrohan419@gmail.com
 */
package com.kittyp.order.model;

import java.math.BigDecimal;

import com.kittyp.order.entity.OrderItemDetails;
import com.kittyp.product.model.ProductModel;

import lombok.Data;

/**
 * @author rrohan419@gmail.com 
 */
@Data
public class OrderItemModel {

	private ProductModel product;
	
	private int quantity;
	
	private BigDecimal price;
	
	private OrderItemDetails itemDetails;
}
