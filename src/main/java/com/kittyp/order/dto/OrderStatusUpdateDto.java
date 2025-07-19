/**
 * @author rrohan419@gmail.com
 */
package com.kittyp.order.dto;

import java.util.Map;

import com.kittyp.order.emus.OrderStatus;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;

/**
 * @author rrohan419@gmail.com 
 */
@Getter
public class OrderStatusUpdateDto {

	@NotNull
	private String orderNumber;
	
	@NotNull
	private OrderStatus orderStatus;
}
