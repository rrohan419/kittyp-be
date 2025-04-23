/**
 * @author rrohan419@gmail.com
 */
package com.kittyp.order.dto;

import com.kittyp.order.emus.OrderStatus;

import lombok.Data;

/**
 * @author rrohan419@gmail.com 
 */
@Data
public class OrderFilterDto {

	private OrderStatus orderStatus;
	
	private String orderNumber;
	
	private String userUuid;
}
