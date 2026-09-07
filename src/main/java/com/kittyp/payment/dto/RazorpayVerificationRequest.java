/**
 * @author rrohan419@gmail.com
 */
package com.kittyp.payment.dto;

import lombok.Getter;
import lombok.Setter;

/**
 * @author rrohan419@gmail.com 
 */
@Getter
@Setter
public class RazorpayVerificationRequest {

	private String orderId;
	
	private String paymentId;
	
	private String signature;
}
