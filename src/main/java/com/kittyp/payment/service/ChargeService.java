/**
 * @author rrohan419@gmail.com
 */
package com.kittyp.payment.service;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import com.kittyp.payment.enums.ChargeType;

/**
 * @author rrohan419@gmail.com 
 */
public interface ChargeService {

	BigDecimal calculateCharge(ChargeType type, BigDecimal baseAmount);
	
	BigDecimal calculateTotalCharges(BigDecimal baseAmount, List<ChargeType> types);
	
	Map<ChargeType, BigDecimal> chargeBreakdown(BigDecimal baseAmount, List<ChargeType> types);
}
