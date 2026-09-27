package com.kittyp.ai.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDate;

import org.junit.jupiter.api.Test;

import com.kittyp.ai.model.TipOfTheDayModel;

class DailyPetTipServiceTest {

	private final DailyPetTipService service = new DailyPetTipService();

	@Test
	void sameDate_returnsSameTip() {
		LocalDate day = LocalDate.of(2026, 9, 27);
		TipOfTheDayModel a = service.tipFor(day);
		TipOfTheDayModel b = service.tipFor(day);
		assertEquals(a.tip(), b.tip());
		assertEquals(day, a.date());
		assertNotNull(a.tip());
		assertTrue(a.tip().length() > 10);
	}

	@Test
	void consecutiveDays_canCycle() {
		LocalDate day = LocalDate.of(2026, 1, 1);
		TipOfTheDayModel first = service.tipFor(day);
		TipOfTheDayModel next = service.tipFor(day.plusDays(1));
		assertNotEquals(first.tip(), next.tip());
	}

	@Test
	void tipCount_isSubstantial() {
		assertTrue(service.tipCount() >= 40);
	}

	@Test
	void nullDay_usesToday() {
		TipOfTheDayModel tip = service.tipFor(null);
		assertEquals(LocalDate.now(), tip.date());
		assertNotNull(tip.tip());
	}
}
