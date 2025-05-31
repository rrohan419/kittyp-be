/**
 * @author rrohan419@gmail.com
 */
package com.kittyp.payment.service;

import com.kittyp.payment.dto.RazorPayOrderRequestDto;
import com.kittyp.payment.dto.RazorpayVerificationRequest;
import com.kittyp.payment.model.CreateOrderModel;
import com.razorpay.RazorpayException;

/**
 * @author rrohan419@gmail.com 
 */
public interface RazorPayService {

	CreateOrderModel createOrder(RazorPayOrderRequestDto orderRequestDto);
	
	String verifyPayment(RazorpayVerificationRequest verificationRequest) throws RazorpayException;

	void handlePaymentTimeout(String orderId);

	void handlePaymentCancellation(String orderId);
}
