/**
 * @author rrohan419@gmail.com
 */
package com.kittyp.order.dto;

import java.math.BigDecimal;
import java.util.List;

import com.kittyp.order.emus.CurrencyType;
import com.kittyp.order.entity.Address;
import com.kittyp.order.entity.Taxes;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;

/**
 * @author rrohan419@gmail.com
 */
@Getter
public class OrderDto {
	
	@NotNull
	private BigDecimal subTotal;

	@NotNull
	private BigDecimal totalAmount;
	
	private CurrencyType currency;

//	@NotNull
	private Address shippingAddress;

//	@NotNull
	private Address billingAddress;
	
	private Taxes taxes;

	@Valid
	private List<OrderItemDto> orderItems;
}
