package com.kittyp.common.util;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Masks phones, emails, and secret-named fields in log text.
 */
public final class PiiMasker {

	private static final Pattern EMAIL = Pattern.compile(
			"([A-Za-z0-9._%+-]+)@([A-Za-z0-9.-]+\\.[A-Za-z]{2,})");
	private static final Pattern E164 = Pattern.compile("\\+\\d{10,15}");
	private static final Pattern SECRET_FIELD = Pattern.compile(
			"(?i)(\"(?:otp|code|password|token|secret)\"\\s*:\\s*\")([^\"]*)(\")");
	private static final Pattern SECRET_EQ = Pattern.compile(
			"(?i)\\b(otp|code|password|token|secret)\\s*[=:]\\s*(\\S+)");

	private PiiMasker() {
	}

	public static String mask(String raw) {
		if (raw == null || raw.isEmpty()) {
			return raw;
		}
		String out = SECRET_FIELD.matcher(raw).replaceAll("$1***$3");
		out = SECRET_EQ.matcher(out).replaceAll("$1=***");
		out = EMAIL.matcher(out).replaceAll(match -> maskEmail(match.group(1), match.group(2)));
		Matcher phones = E164.matcher(out);
		StringBuffer buf = new StringBuffer();
		while (phones.find()) {
			phones.appendReplacement(buf, Matcher.quoteReplacement(maskPhone(phones.group())));
		}
		phones.appendTail(buf);
		return buf.toString();
	}

	public static String maskEmail(String email) {
		if (email == null || email.isBlank()) {
			return email;
		}
		int at = email.indexOf('@');
		if (at <= 0) {
			return "***";
		}
		return maskEmail(email.substring(0, at), email.substring(at + 1));
	}

	private static String maskEmail(String local, String domain) {
		String first = local.isEmpty() ? "*" : local.substring(0, 1);
		return first + "***@" + domain;
	}

	public static String maskPhone(String phone) {
		if (phone == null) {
			return null;
		}
		String digits = phone.replaceAll("\\D", "");
		if (digits.length() <= 4) {
			return "****";
		}
		if (digits.length() >= 12 && digits.startsWith("91")) {
			return "+91" + digits.substring(2, 4) + "******" + digits.substring(digits.length() - 2);
		}
		if (digits.length() == 10) {
			return "+91" + digits.substring(0, 2) + "******" + digits.substring(8);
		}
		String last2 = digits.substring(digits.length() - 2);
		String prefix = digits.substring(0, Math.min(4, digits.length() - 2));
		return "+" + prefix + "******" + last2;
	}
}
