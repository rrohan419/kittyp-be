package com.kittyp.ai.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import com.kittyp.ai.entity.NutritionPlan;
import com.kittyp.ai.enums.NutritionPlanStatus;
import com.kittyp.ai.model.NutritionPlanModel;
import com.kittyp.ai.repository.NutritionPlanRepository;
import com.kittyp.common.util.Mapper;
import com.kittyp.nutrition.service.PetDailyPlanService;
import com.kittyp.user.entity.User;
import com.kittyp.user.repository.UserRepository;
import com.kittyp.user.service.PetAccessGuard;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class NutritionPlanSendDurationTest {

	@Mock
	private NutritionPlanRepository nutritionPlanRepository;
	@Mock
	private Mapper mapper;
	@Mock
	private PetDailyPlanService petDailyPlanService;
	@Mock
	private PetAccessGuard petAccessGuard;
	@Mock
	private UserRepository userRepository;

	@InjectMocks
	private NutritionPlanServiceImpl service;

	private NutritionPlan plan;
	private User doctor;

	@BeforeEach
	void setUp() {
		doctor = User.builder().uuid("doc-1").email("doc@test.com").build();
		plan = NutritionPlan.builder()
				.uuid("plan-1")
				.petUuid("6UP32B")
				.userUuid("parent-1")
				.parentUserUuid("parent-1")
				.status(NutritionPlanStatus.DRAFT)
				.planName("Milo plan")
				.build();
		when(nutritionPlanRepository.findByUuid("plan-1")).thenReturn(Optional.of(plan));
		when(userRepository.findByUuid("doc-1")).thenReturn(Optional.of(doctor));
		when(nutritionPlanRepository.save(any(NutritionPlan.class))).thenAnswer(inv -> inv.getArgument(0));
		when(mapper.convert(any(NutritionPlan.class), eq(NutritionPlanModel.class))).thenAnswer(inv -> {
			NutritionPlan saved = inv.getArgument(0);
			NutritionPlanModel model = new NutritionPlanModel();
			model.setUuid(saved.getUuid());
			model.setDurationDays(saved.getDurationDays());
			return model;
		});
	}

	@Test
	void requestedDaysArePersisted() {
		NutritionPlanModel model = service.sendPlan("plan-1", "doc-1", 14);
		assertEquals(14, model.getDurationDays());
		assertEquals(14, plan.getDurationDays());
		assertEquals(NutritionPlanStatus.SENT, plan.getStatus());
	}

	@Test
	void missingDaysDefaultToThirty() {
		assertEquals(30, service.sendPlan("plan-1", "doc-1", null).getDurationDays());
	}

	@Test
	void durationIsClamped() {
		assertEquals(30, NutritionPlanServiceImpl.resolveDurationDays(null));
		assertEquals(30, NutritionPlanServiceImpl.resolveDurationDays(0));
		assertEquals(7, NutritionPlanServiceImpl.resolveDurationDays(7));
		assertEquals(90, NutritionPlanServiceImpl.resolveDurationDays(120));
	}

	@Test
	void clinicalAccessIsCheckedBeforeSend() {
		service.sendPlan("plan-1", "doc-1", 7);
		verify(petAccessGuard).requireClinicalAccess(doctor, "6UP32B");
	}
}
