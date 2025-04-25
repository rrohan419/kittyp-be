/**
 * @author rrohan419@gmail.com
 */
package com.kittyp.payment.dto;

import java.math.BigDecimal;
import java.util.List;

import com.kittyp.order.entity.Taxes;
import com.kittyp.payment.enums.CurrencySymbol;

import lombok.Getter;
import lombok.Setter;

/**
 * @author rrohan419@gmail.com 
 */
@Setter
@Getter
public class RazorPayOrderRequestDto {

	private BigDecimal amount;
	
	private CurrencySymbol currency;
	
	private String receipt;
	
	private List<String> notes;
	
	private Taxes taxes;
	
	
}
