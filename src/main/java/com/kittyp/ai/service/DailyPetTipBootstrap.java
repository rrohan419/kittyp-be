package com.kittyp.ai.service;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

import com.kittyp.ai.entity.DailyPetTip;
import com.kittyp.ai.repository.DailyPetTipRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/** Seeds curated tip rows; inserts any missing seed texts so existing DBs reach ~100. */
@Slf4j
@Component
@RequiredArgsConstructor
public class DailyPetTipBootstrap implements ApplicationRunner {

	static final List<String> SEED = List.of(
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
			"End the day with a quiet check-in: food, water, comfort, and a kind word.",
			"A lick mat with a smear of pet-safe spread turns restlessness into calm focus.",
			"Teach a 'settle' on a mat so busy households still have a peaceful landing zone.",
			"Cats love vertical space: a sturdy cat tree can reduce night-time zoomies.",
			"Morning sniffari in the yard beats a rushed loop around the block.",
			"Keep a towel by the door for rainy paws — a gentle wipe is a daily kindness.",
			"Frozen broth cubes (plain, pet-safe) make warm days extra special.",
			"Let cats 'hunt' meals with wand toys before food to satisfy natural sequences.",
			"A second water station upstairs or downstairs encourages extra sips.",
			"Massage along the shoulders after walks can help tight muscles relax.",
			"Clicker or marker words make communication clearer than repeating a name.",
			"Window bird-watching is TV for cats — leave a safe perch and a screen.",
			"Practice polite leash skills in quiet streets before busy parks.",
			"Swap one treat for a vegetable your vet approves — variety without extra calories.",
			"Microchip and collar ID together: two ways home if a curious explorer slips out.",
			"Slow blinking at cats is a friendly 'I mean well' you can share across the room.",
			"Dogs often love a job: carrying a light bag or finding a hidden toy.",
			"Keep claws blunt with scratchers placed where cats already stretch.",
			"A consistent bedtime snack-free wind-down helps overnight rest.",
			"Invite sniffing new (safe) objects on a tray — novelty without leaving home.",
			"Brush teeth in tiny steps: smell paste, touch lip, then one tooth, then praise.",
			"Cool tile or a damp cloth on hot days beats asking pets to 'tough it out'.",
			"Track one happy habit: daily play minute, weekly brush, monthly nail peek.",
			"Let shy pets approach guests on their terms; forced greetings backfire.",
			"A second litter box (plus one) reduces stress in multi-cat homes.",
			"Tug on the floor with rules — drop means the game pauses, then restarts.",
			"Scent walks: hide treats along a hallway and narrate the treasure hunt.",
			"Keep vet-ready: a familiar carrier left out with blankets feels less scary.",
			"Rainy-day fetch down a hallway still counts as joyful movement.",
			"Watch weight with a rib-check once a month; adjust meals with your clinic.",
			"Talk through grooming so the dryer and clippers never feel like surprises.",
			"Rotate three toys in and out of a box so yesterday's favorite feels new.",
			"A dawn or dusk walk matches many pets' natural energy peaks.",
			"Offer a lick of wet food on a spoon as a tiny 'I see you' between meals.",
			"Teach chin rest for easier eye drops and ear checks later in life.",
			"Keep houseplants out of reach; many common leaves are not pet-safe.",
			"Socialize puppies and kittens to gentle handling of paws and mouths early.",
			"A fan on low plus shade is kinder than closed rooms in heat.",
			"End training on a win — one easy cue — so tomorrow starts eager.",
			"Harlem shake? Skip it. A slow stretch beside your pet is enough yoga.",
			"Record a baseline of eating and drinking so changes stand out sooner.",
			"Give senior pets extra time to stand, turn, and climb without rushing.",
			"Cats often prefer shallow, wide bowls that don't squeeze whiskers.",
			"Practice recall with jackpot treats in the garden before off-leash dreams.",
			"A white-noise clip at night can mask fireworks season rumble.",
			"Share the sofa on their terms: a blanket 'spot' they can claim.",
			"Thank-you scratches after medicine make clinic follow-ups easier next time.",
			"Keep play gentle with small pets: support the body, never dangle.",
			"One new walking route a week keeps neighborhood smells interesting.",
			"If they startle, pause and breathe together — safety is the real treat.");

	private final DailyPetTipRepository dailyPetTipRepository;

	@Override
	public void run(ApplicationArguments args) {
		Set<String> existing = new HashSet<>(dailyPetTipRepository.findAllTipsOrdered());
		List<DailyPetTip> missing = SEED.stream()
				.filter(tip -> !existing.contains(tip))
				.map(tip -> {
					DailyPetTip row = new DailyPetTip();
					row.setTip(tip);
					return row;
				})
				.toList();
		if (missing.isEmpty()) {
			return;
		}
		dailyPetTipRepository.saveAll(missing);
		log.info("Seeded {} daily pet tips (table now ~{})", missing.size(), existing.size() + missing.size());
	}
}
