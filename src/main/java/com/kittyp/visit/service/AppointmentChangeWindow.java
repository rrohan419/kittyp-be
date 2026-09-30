package com.kittyp.visit.service;

import java.time.Duration;
import java.time.LocalDateTime;

/**
 * Pet owners may reschedule or cancel only when at least six hours remain before the visit.
 * Clinic staff are not checked against this window.
 */
public final class AppointmentChangeWindow {

	public static final Duration MINIMUM_NOTICE = Duration.ofHours(6);

	private AppointmentChangeWindow() {
	}

	public static boolean allowed(LocalDateTime slotStart, LocalDateTime nowClinic) {
		if (slotStart == null || nowClinic == null) {
			return false;
		}
		return Duration.between(nowClinic, slotStart).compareTo(MINIMUM_NOTICE) >= 0;
	}

	public static LocalDateTime now(String timezone) {
		return DoctorHours.nowLocal(timezone);
	}

	public static String zone(String bookingZone, String clinicZone) {
		if (bookingZone != null && !bookingZone.isBlank()) {
			return bookingZone.trim();
		}
		if (clinicZone != null && !clinicZone.isBlank()) {
			return clinicZone.trim();
		}
		return DoctorHours.DEFAULT_ZONE;
	}
}
