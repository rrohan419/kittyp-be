/**
 * @author rrohan419@gmail.com
 */
package com.kittyp.payment.dto;

import java.util.List;

import com.kittyp.payment.enums.CurrencySymbol;

import lombok.Getter;
import lombok.Setter;

/**
 * @author rrohan419@gmail.com 
 */
@Setter
@Getter
public class RazorPayOrderRequestDto {

	private Long amount;
	
	private CurrencySymbol currency;
	
	private String recipt;
	
	private List<String> notes;
}
