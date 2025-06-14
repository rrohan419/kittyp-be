/**
 * @author rrohan419@gmail.com
 */
package com.kittyp.order.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import com.kittyp.order.emus.CurrencyType;
import com.kittyp.order.emus.OrderStatus;
import com.kittyp.order.entity.Address;
import com.kittyp.order.entity.Taxes;
import com.kittyp.user.models.UserDetailsModel;

import lombok.Data;

/**
 * @author rrohan419@gmail.com 
 */
@Data
public class OrderModel {

	private String orderNumber;
	
	private LocalDateTime createdAt;
	
	private String aggregatorOrderNumber;
	
	private BigDecimal totalAmount;
	
	private BigDecimal subTotal;
	
	private CurrencyType currency;
	
	private OrderStatus status;
	
	private Taxes taxes;
	
	private Address shippingAddress;
	
	private Address billingAddress;
	
	private List<OrderItemModel> orderItems;

	private UserDetailsModel user;
}
