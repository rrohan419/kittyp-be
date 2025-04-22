/**
 * @author rrohan419@gmail.com
 */
package com.kittyp.order.dto;

import java.math.BigDecimal;

import com.kittyp.order.entity.OrderItemDetails;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;

/**
 * @author rrohan419@gmail.com
 */
@Getter
public class OrderItemDto {

	@NotBlank
	private String productUuid;
	
	@NotNull
	private int quantity;

	@NotNull
	private BigDecimal price;

	private OrderItemDetails itemDetails;
}
