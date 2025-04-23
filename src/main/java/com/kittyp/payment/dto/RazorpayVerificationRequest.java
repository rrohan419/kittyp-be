/**
 * @author rrohan419@gmail.com
 */
package com.kittyp.payment.dto;

import lombok.Getter;

/**
 * @author rrohan419@gmail.com 
 */
@Getter
public class RazorpayVerificationRequest {

	private String orderId;
	
	private String paymentId;
	
	private String signature;
}
