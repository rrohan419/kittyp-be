package com.kittyp.notification.service;

/**
 * Pluggable SMS delivery for OTP codes.
 * Local/dev uses a logging implementation; production can bind Twilio (or similar).
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
