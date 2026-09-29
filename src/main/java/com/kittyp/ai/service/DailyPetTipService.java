package com.kittyp.ai.service;

import java.time.LocalDate;
import java.util.List;

import org.springframework.stereotype.Service;

import com.kittyp.ai.model.TipOfTheDayModel;
import com.kittyp.ai.repository.DailyPetTipRepository;

import lombok.RequiredArgsConstructor;

/**
 * Global tip-of-the-day from {@code daily_pet_tips} (same tip for everyone each calendar day).
 */
@Service
@RequiredArgsConstructor
public class DailyPetTipService {

	private static final String FALLBACK =
			"Fresh water every day keeps pets happier — rinse bowls morning and night.";

	private final DailyPetTipRepository dailyPetTipRepository;

	public TipOfTheDayModel tipFor(LocalDate day) {
		LocalDate date = day != null ? day : LocalDate.now();
		List<String> tips = dailyPetTipRepository.findAllTipsOrdered();
		if (tips == null || tips.isEmpty()) {
			return new TipOfTheDayModel(FALLBACK, date);
		}
		int index = Math.floorMod(date.toEpochDay(), tips.size());
		return new TipOfTheDayModel(tips.get(index), date);
	}

	public TipOfTheDayModel tipForToday() {
		return tipFor(LocalDate.now());
	}

	/** Visible for tests. */
	int tipCount() {
		return (int) dailyPetTipRepository.count();
	}
}
