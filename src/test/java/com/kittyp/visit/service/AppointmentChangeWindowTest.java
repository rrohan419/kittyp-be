package com.kittyp.visit.service;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDateTime;

import org.junit.jupiter.api.Test;

class AppointmentChangeWindowTest {

	private static final LocalDateTime NOW = LocalDateTime.of(2026, 10, 1, 10, 0);

	@Test
	void allowsTwentyFourHoursAndExactSixHours() {
		assertTrue(AppointmentChangeWindow.allowed(NOW.plusHours(24), NOW));
		assertTrue(AppointmentChangeWindow.allowed(NOW.plusHours(6), NOW));
	}

	@Test
	void rejectsFiveHoursFiftyNineMinutes() {
		assertFalse(AppointmentChangeWindow.allowed(NOW.plusHours(6).minusMinutes(1), NOW));
	}
}
