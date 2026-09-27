package com.kittyp.ai.service;

import java.time.LocalDate;
import java.util.List;

import org.springframework.stereotype.Service;

import com.kittyp.ai.model.TipOfTheDayModel;

/**
 * Global tip-of-the-day: same curated tip for every user on a given calendar day.
 */
@Service
public class DailyPetTipService {

	private static final List<String> TIPS = List.of(
			"Fresh water every day keeps pets happier — rinse bowls morning and night.",
			"A short play session before meals can reduce begging and support a healthy weight.",
			"Gentle petting and calm talk strengthen the bond you share with your companion.",
			"Sniff walks are enrichment: let curious noses explore at their own pace.",
			"Rotating toys weekly keeps playtime exciting without buying something new each day.",
			"A cozy resting spot away from noise helps pets recharge after busy days.",
			"Soft praise and treats make training feel like a game, not a chore.",
			"Regular brushing spreads love and catches early coat or skin changes.",
			"Cats often prefer elevated perches — a window seat can be their favorite TV.",
			"Dogs thrive on routine: consistent meal and walk times lower everyday stress.",
			"Puzzle feeders turn dinner into brain exercise and slow down gulping.",
			"A few minutes of floor time with toys burns energy and builds trust.",
			"Clean litter boxes and fresh bedding are small comforts that mean a lot.",
			"Cheek and chin rubs (where they like them) release happy bonding hormones.",
			"Outdoor time in safe spaces supports mood — supervised and weather-aware.",
			"Keep nail trims gentle and gradual; reward calm paws with something tasty.",
			"Reading your pet's body language — soft eyes, relaxed ears — deepens understanding.",
			"Hydration tip: some pets drink more from a quiet fountain than a still bowl.",
			"Hide-and-seek with treats turns your home into a friendly adventure park.",
			"Warm blankets and soft beds matter most on cooler evenings.",
			"Positive first visits to the clinic start with treats and short, happy trips.",
			"Social pets enjoy calm company; quiet pets need alone time — both are valid.",
			"Chewing appropriate toys satisfies natural instincts and protects furniture.",
			"A daily check of eyes, ears, and paws during cuddles catches little issues early.",
			"Species-appropriate play: chase for cats, fetch or tug for many dogs.",
			"Sharing calm presence — sitting nearby while they rest — is quality time too.",
			"Fresh air and daylight help circadian rhythms for people and pets alike.",
			"Slow introductions to new people or pets keep everyone feeling safe and curious.",
			"Grooming sessions can become bonding rituals with patience and soft voices.",
			"Training a simple 'touch' or 'sit' builds confidence for both of you.",
			"Scatter feeding on a mat encourages natural foraging without extra gear.",
			"Celebrate small wins: a calm greeting, a finished meal, a restful nap.",
			"Pets notice your mood — gentle routines help them feel secure when days are hectic.",
			"Rotate resting spots seasonally: cooler floors in heat, warmer nests in chill.",
			"Name games and find-it cues make learning feel like play.",
			"Dental chews and tooth-friendly toys support a brighter smile over time.",
			"Watch for happy signals after walks: soft pant, wag, or contented stretch.",
			"A predictable goodbye and hello ritual eases separation for many companions.",
			"Enrichment boxes with paper and safe scents spark curiosity indoors.",
			"Thank your pet with a scratch in their favorite spot — they often return the love.",
			"Balanced meals and measured treats leave room for joyful training rewards.",
			"Soft music or white noise can soothe noise-sensitive animals at home.",
			"Sunbeam naps are a luxury — open curtains for natural light and warmth.",
			"Short training bursts (1–3 minutes) beat long drills for focus and fun.",
			"Respect 'no thanks' moments: walking away from pets who need space builds trust.",
			"Seasonal flea and parasite plans keep outdoor adventures worry-lighter.",
			"Carry water on longer outings — shared hydration breaks are bonding too.",
			"Photo days and silly videos freeze happy moments you'll both want to revisit.",
			"A calm crate or hidey spot is a den, not a punishment — make it inviting.",
			"Learn one new enrichment idea a month; variety keeps minds bright.",
			"Pets age gracefully with joint-friendly play and shorter, more frequent walks.",
			"End the day with a quiet check-in: food, water, comfort, and a kind word.");

	public TipOfTheDayModel tipFor(LocalDate day) {
		LocalDate date = day != null ? day : LocalDate.now();
		int index = Math.floorMod(date.toEpochDay(), TIPS.size());
		return new TipOfTheDayModel(TIPS.get(index), date);
	}

	public TipOfTheDayModel tipForToday() {
		return tipFor(LocalDate.now());
	}

	/** Visible for tests. */
	int tipCount() {
		return TIPS.size();
	}
}
