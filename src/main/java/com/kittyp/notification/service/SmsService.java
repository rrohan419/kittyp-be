package com.kittyp.notification.service;

/**
 * SMS OTP delivery. Live bean is SmsGatewayService (SMS Gate POST /message).
 */
public interface SmsService {

	/**
	 * Deliver a one-time password to the given E.164-ish phone number.
	 *
	 * @param phoneNumber destination phone (e.g. +919876543210)
	 * @param code        6-digit OTP
	 */
	void sendOtp(String phoneNumber, String code);

	/**
	 * Same as {@link #sendOtp(String, String)} with email failover when the phone gateway fails.
	 *
	 * @return {@code true} if the OTP was delivered via email failover instead of SMS
	 */
	default boolean sendOtp(String phoneNumber, String code, String fallbackEmail) {
		sendOtp(phoneNumber, code);
		return false;
	}
}
