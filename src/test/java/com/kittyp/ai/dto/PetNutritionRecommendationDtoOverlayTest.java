package com.kittyp.ai.dto;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDate;
import java.util.Set;

import org.junit.jupiter.api.Test;

import com.kittyp.user.entity.Pet;

class PetNutritionRecommendationDtoOverlayTest {

	@Test
	void fillsBlankFieldsFromStoredPet() {
		Pet pet = Pet.builder()
				.uuid("6UP32B")
				.name("Racer")
				.type("dog")
				.breed("indian")
				.dateOfBirth(LocalDate.of(2021, 3, 8))
				.weight("18")
				.gender("male")
				.activityLevel("high")
				.currentFoodBrand("Royal Canin")
				.healthConditions("hip dysplasia")
				.allergies("chicken")
				.isNeutered(true)
				.profilePicture("https://cdn.example/racer.jpg")
				.photos(Set.of())
				.build();

		PetNutritionRecommendationDto dto = new PetNutritionRecommendationDto();
		dto.setUuid("6up32b");
		dto.setName("Racer");
		dto.overlayFromStoredPet(pet);

		assertEquals("Racer", dto.getName());
		assertEquals("dog", dto.getType());
		assertEquals("18", dto.getWeight());
		assertEquals("2021-03-08", dto.getDateOfBirth());
		assertEquals("male", dto.getGender());
		assertEquals("Royal Canin", dto.getCurrentFoodBrand());
		assertEquals("hip dysplasia", dto.getHealthConditions());
		assertEquals("chicken", dto.getAllergies());
		assertTrue(dto.getIsSpayedOrNeutered());
		assertEquals("https://cdn.example/racer.jpg", dto.getProfilePicture());
	}
}
