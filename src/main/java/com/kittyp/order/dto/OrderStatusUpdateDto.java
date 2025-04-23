/**
 * @author rrohan419@gmail.com
 */
package com.kittyp.order.dto;

import java.util.Map;

import com.kittyp.order.emus.OrderStatus;

import lombok.Getter;

/**
 * @author rrohan419@gmail.com 
 */
@Getter
public class OrderStatusUpdateDto {

	private String orderNumber;
	private OrderStatus orderStatus;
}
