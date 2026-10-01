package com.kittyp.support.service;

import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Support ids use Asia/Kolkata, the same zone as clinic hours and user mail.
 */
public final class SupportIds {

	public static final ZoneId ZONE = ZoneId.of("Asia/Kolkata");
	public static final int SUBJECT_MAX = 500;
	public static final int BODY_MAX = 8000;
	static final long ADVISORY_LOCK_KEY = 58291011L;

	private static final Pattern ID = Pattern.compile("KIT-\\d{8}-\\d{4}");
	private static final DateTimeFormatter DAY = DateTimeFormatter.BASIC_ISO_DATE;

	private SupportIds() {
	}

	public static String format(LocalDate date, int sequence) {
		if (sequence < 1 || sequence > 9999) {
			throw new IllegalArgumentException("Support id sequence out of range");
		}
		return "KIT-" + date.format(DAY) + "-" + String.format("%04d", sequence);
	}

	public static String prefix(LocalDate date) {
		return "KIT-" + date.format(DAY) + "-%";
	}

	public static String findInSubject(String subject) {
		if (subject == null || subject.isBlank()) {
			return null;
		}
		Matcher matcher = ID.matcher(subject);
		return matcher.find() ? matcher.group() : null;
	}

	public static String cap(String value, int max) {
		if (value == null) {
			return "";
		}
		String trimmed = value.trim();
		if (trimmed.length() <= max) {
			return trimmed;
		}
		return trimmed.substring(0, max);
	}
}
