package com.kittyp.support.service;

import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Support ids use Asia/Kolkata, the same zone as clinic hours and user mail.
 * The customer-facing form is {@code KIT-YYYYMMDD-#####}, sequence 1 through 99999.
 */
public final class SupportIds {

	public static final ZoneId ZONE = ZoneId.of("Asia/Kolkata");
	static final long ADVISORY_LOCK_KEY = 58291011L;

	private static final Pattern ID = Pattern.compile("KIT-\\d{8}-\\d{5}(?!\\d)");
	private static final DateTimeFormatter DAY = DateTimeFormatter.BASIC_ISO_DATE;
	private static final int SEQUENCE_MAX = 99999;

	private SupportIds() {
	}

	public static String format(LocalDate date, int sequence) {
		if (sequence < 1 || sequence > SEQUENCE_MAX) {
			throw new IllegalArgumentException("Support id sequence out of range");
		}
		return "KIT-" + date.format(DAY) + "-" + String.format("%05d", sequence);
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
}
