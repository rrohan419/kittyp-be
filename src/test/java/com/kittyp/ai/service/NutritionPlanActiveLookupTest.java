package com.kittyp.ai.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

import com.kittyp.ai.entity.NutritionPlan;
import com.kittyp.ai.enums.NutritionPlanStatus;
import com.kittyp.ai.model.NutritionPlanModel;
import com.kittyp.ai.repository.NutritionPlanRepository;
import com.kittyp.common.exception.CustomException;
import com.kittyp.common.util.Mapper;
import com.kittyp.user.entity.Pet;
import com.kittyp.user.repository.PetsRepository;

@ExtendWith(MockitoExtension.class)
class NutritionPlanActiveLookupTest {

	@Mock
	private NutritionPlanRepository nutritionPlanRepository;
	@Mock
	private PetsRepository petsRepository;
	@Mock
	private Mapper mapper;

	@InjectMocks
	private NutritionPlanServiceImpl service;

	@Test
	void missingSentPlanIsNotFoundNotServerError() {
		when(petsRepository.findByUuidIgnoreCase("Y9KJKW")).thenReturn(Optional.empty());
		when(nutritionPlanRepository.findFirstByPetUuidAndStatusAndIsActiveTrueOrderBySentAtDesc(
				"Y9KJKW", NutritionPlanStatus.SENT)).thenReturn(Optional.empty());

		CustomException ex = assertThrows(CustomException.class,
				() -> service.getActiveSentPlanForPet("Y9KJKW"));
		assertEquals(HttpStatus.NOT_FOUND, ex.getHttpStatus());
		assertEquals("No sent nutrition plan found for this pet", ex.getMessage());
	}

	@Test
	void looksUpSentPlanByCanonicalPublicPetId() {
		Pet pet = Pet.builder().uuid("Y9KJKW").name("Milo").build();
		NutritionPlan plan = NutritionPlan.builder().uuid("plan-1").petUuid("Y9KJKW").status(NutritionPlanStatus.SENT)
				.build();
		NutritionPlanModel model = new NutritionPlanModel();
		model.setUuid("plan-1");
		when(petsRepository.findByUuidIgnoreCase("y9kjkw")).thenReturn(Optional.of(pet));
		when(nutritionPlanRepository.findFirstByPetUuidAndStatusAndIsActiveTrueOrderBySentAtDesc(
				"Y9KJKW", NutritionPlanStatus.SENT)).thenReturn(Optional.of(plan));
		when(mapper.convert(eq(plan), eq(NutritionPlanModel.class))).thenReturn(model);

		assertEquals("plan-1", service.getActiveSentPlanForPet("y9kjkw").getUuid());
		verify(nutritionPlanRepository).findFirstByPetUuidAndStatusAndIsActiveTrueOrderBySentAtDesc(
				"Y9KJKW", NutritionPlanStatus.SENT);
	}
}
