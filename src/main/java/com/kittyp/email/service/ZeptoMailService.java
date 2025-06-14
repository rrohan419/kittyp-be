/**
 * @author rrohan419@gmail.com
 */
package com.kittyp.email.service;

/**
 * @author rrohan419@gmail.com 
 */
public interface ZeptoMailService {

	void sendWelcomeEmail(String recipientEmail);
	
	void sendPasswordResetCode(String email);

	void sendOrderConfirmationEmail(String recipientEmail, String orderNumber);
}
