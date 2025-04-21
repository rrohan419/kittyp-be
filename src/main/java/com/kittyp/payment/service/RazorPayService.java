/**
 * @author rrohan419@gmail.com
 */
package com.kittyp.payment.service;

import com.kittyp.payment.dto.RazorPayOrderRequestDto;
import com.kittyp.payment.model.CreateOrderModel;

/**
 * @author rrohan419@gmail.com 
 */
public interface RazorPayService {

	CreateOrderModel createOrder(RazorPayOrderRequestDto orderRequestDto);
}
