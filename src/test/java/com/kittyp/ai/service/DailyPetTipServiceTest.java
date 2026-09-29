package com.kittyp.ai.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDate;
import java.util.HashSet;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import static org.mockito.Mockito.when;

import com.kittyp.ai.model.TipOfTheDayModel;
import com.kittyp.ai.repository.DailyPetTipRepository;

@ExtendWith(MockitoExtension.class)
class DailyPetTipServiceTest {

	@Mock
	private DailyPetTipRepository dailyPetTipRepository;

	private DailyPetTipService service;

	@BeforeEach
	void setUp() {
		service = new DailyPetTipService(dailyPetTipRepository);
	}

	@Test
	void sameDate_returnsSameTip() {
		when(dailyPetTipRepository.findAllTipsOrdered()).thenReturn(List.of("A", "B", "C"));
		LocalDate day = LocalDate.of(2026, 9, 27);
		TipOfTheDayModel a = service.tipFor(day);
		TipOfTheDayModel b = service.tipFor(day);
		assertEquals(a.tip(), b.tip());
		assertEquals(day, a.date());
		assertNotNull(a.tip());
	}

	@Test
	void consecutiveDays_canCycle() {
		when(dailyPetTipRepository.findAllTipsOrdered()).thenReturn(List.of("A", "B", "C"));
		LocalDate day = LocalDate.of(2026, 1, 1);
		assertNotEquals(service.tipFor(day).tip(), service.tipFor(day.plusDays(1)).tip());
	}

	@Test
	void emptyTable_usesFallback() {
		when(dailyPetTipRepository.findAllTipsOrdered()).thenReturn(List.of());
		TipOfTheDayModel tip = service.tipFor(LocalDate.now());
		assertNotNull(tip.tip());
		assertEquals(LocalDate.now(), tip.date());
	}

	@Test
	void seedList_isAboutOneHundredUnique() {
		assertEquals(DailyPetTipBootstrap.SEED.size(), new HashSet<>(DailyPetTipBootstrap.SEED).size());
		assertTrue(DailyPetTipBootstrap.SEED.size() >= 100);
	}
}
