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
}
