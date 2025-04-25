/**
 * @author rrohan419@gmail.com
 */
package com.kittyp.order.entity;

import java.io.Serializable;
import java.math.BigDecimal;

import lombok.Getter;
import lombok.Setter;

/**
 * @author rrohan419@gmail.com 
 */
@Getter
@Setter
public class Taxes implements Serializable {

	private static final long serialVersionUID = 1L;

	private BigDecimal serviceCharge;
	private BigDecimal shippingCharges;
}
