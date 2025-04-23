/**
 * @author rrohan419@gmail.com
 */
package com.kittyp.order.model;

import java.math.BigDecimal;
import java.util.List;

import com.kittyp.order.emus.CurrencyType;
import com.kittyp.order.emus.OrderStatus;
import com.kittyp.order.entity.Address;

import lombok.Data;

/**
 * @author rrohan419@gmail.com 
 */
@Data
public class OrderModel {

	private String orderNumber; 
	
	private BigDecimal totalAmount;
	
	private CurrencyType currency;
	
	private OrderStatus status;
	
	private Address shippingAddress;
	
	private Address billingAddress;
	
	private List<OrderItemModel> orderItems;
}
