/**
 * @author rrohan419@gmail.com
 */
package com.kittyp.payment.service;

import com.kittyp.payment.model.RazorpayResponseModel;

/**
 * @author rrohan419@gmail.com 
 */
public interface WebhookService {

	void razorpayWebbhook(RazorpayResponseModel razorpayResponseModel);
}
