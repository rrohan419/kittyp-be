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

	/** Sends a signup OTP email. purpose e.g. "email" or "phone". */
	void sendSignupOtpEmail(String recipientEmail, String code, String purpose, String phoneHint);

	/** Clinic doctor invitation with accept link. */
	void sendClinicDoctorInviteEmail(String recipientEmail, String doctorName, String clinicName, String acceptUrl);
}
