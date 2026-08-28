package com.kittyp.ai.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.modelmapper.ModelMapper;
import org.springframework.mock.env.MockEnvironment;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.kittyp.ai.entity.NutritionPlan;
import com.kittyp.ai.enums.NutritionPlanStatus;
import com.kittyp.ai.model.NutritionPlanModel;
import com.kittyp.common.util.Mapper;

class NutritionPlanToModelTest {

	private NutritionPlanServiceImpl service;

	@BeforeEach
	void setUp() {
		Mapper mapper = new Mapper(new ModelMapper(), new ObjectMapper(), new MockEnvironment());
		mapper.init();
		service = new NutritionPlanServiceImpl(null, null, mapper, null, null, null, null);
	}

	@Test
	void toModelMapsNestedRecommendationNotRawJsonColumns() {
		NutritionPlan plan = NutritionPlan.builder()
				.uuid("plan-1")
				.petUuid("pet-1")
				.userUuid("user-1")
				.parentUserUuid("parent-1")
				.doctorUserUuid("doc-1")
				.status(NutritionPlanStatus.APPROVED)
				.planName("Test plan")
				.petProfileSummary("{\"name\":\"Milo\",\"type\":\"DOG\"}")
				.environmentalImpact("{}")
				.dailyFeedingPlan("{\"caloriesPerDay\":500,\"meals\":[],\"supplements\":[]}")
				.specialConsiderations("[]")
				.recommendedProducts("[]")
				.longTermWellnessTips("[\"Walk daily\"]")
				.environment("{}")
				.build();

		NutritionPlanModel model = service.toModel(plan);

		assertEquals("plan-1", model.getUuid());
		assertEquals("APPROVED", model.getStatus());
		assertNotNull(model.getNutritionRecommendationResponse());
		assertEquals("Milo", model.getNutritionRecommendationResponse().getPetProfileSummary().getName());
		assertEquals(500, model.getNutritionRecommendationResponse().getDailyFeedingPlan().getCaloriesPerDay());
		assertEquals(1, model.getNutritionRecommendationResponse().getLongTermWellnessTips().size());
		assertNull(model.getNotes());
	}
}
