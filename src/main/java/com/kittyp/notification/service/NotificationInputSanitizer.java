package com.kittyp.notification.service;

/**
 * Rejects CR/LF and obviously invalid notification addresses before HTTP clients.
 */
public final class NotificationInputSanitizer {

	private NotificationInputSanitizer() {
	}

	public static void rejectCrLf(String value, String field) {
		if (value != null && (value.indexOf('\r') >= 0 || value.indexOf('\n') >= 0)) {
			throw new IllegalArgumentException(field + " contains illegal line break characters");
		}
	}

	public static String requireE164Phone(String phoneNumber) {
		rejectCrLf(phoneNumber, "phone");
		if (phoneNumber == null || phoneNumber.isBlank()) {
			throw new IllegalArgumentException("Phone number is required");
		}
		String digits = phoneNumber.replaceAll("\\D", "");
		if (digits.length() < 10) {
			throw new IllegalArgumentException("Phone number is invalid");
		}
		String local10 = digits.substring(digits.length() - 10);
		if (!local10.matches("\\d{10}")) {
			throw new IllegalArgumentException("Phone number is invalid");
		}
		return "+91" + local10;
	}

	public static String requireOtp(String otpCode) {
		rejectCrLf(otpCode, "otp");
		if (otpCode == null || otpCode.isBlank()) {
			throw new IllegalArgumentException("OTP code is required");
		}
		return otpCode.trim();
	}

	public static String requireEmail(String email) {
		rejectCrLf(email, "email");
		if (email == null || email.isBlank()) {
			throw new IllegalArgumentException("Email is required");
		}
		return email.trim();
	}
}
