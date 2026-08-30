package com.kittyp.nutrition.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import com.kittyp.common.dto.ApiResponse;
import com.kittyp.common.dto.SuccessResponse;
import com.kittyp.nutrition.dao.PetFeedingLogDao;
import com.kittyp.nutrition.dto.PetFeedingLogRequest;
import com.kittyp.nutrition.entity.PetFeedingLog;
import com.kittyp.nutrition.enums.FeedingStatus;
import com.kittyp.user.dao.UserDao;
import com.kittyp.user.entity.User;
import com.kittyp.user.service.PetAccessGuard;

@ExtendWith(MockitoExtension.class)
class PetFeedingLogControllerTest {

	@Mock
	private ApiResponse<?> responseBuilder;
	@Mock
	private PetFeedingLogDao petFeedingLogDao;
	@Mock
	private UserDao userDao;
	@Mock
	private PetAccessGuard petAccessGuard;

	@InjectMocks
	private PetFeedingLogController controller;

	private User owner;

	@BeforeEach
	void setUp() {
		owner = User.builder().uuid("owner-1").email("owner@test.com").build();
		SecurityContextHolder.getContext()
				.setAuthentication(new UsernamePasswordAuthenticationToken("owner@test.com", "x"));
		when(userDao.userByEmail("owner@test.com")).thenReturn(owner);
		when(responseBuilder.buildSuccessResponse(any(), any(), any()))
				.thenReturn(ResponseEntity.ok(new SuccessResponse<>()));
	}

	@AfterEach
	void clearAuth() {
		SecurityContextHolder.clearContext();
	}

	@Test
	void getUsesCanonicalPublicPetId() {
		when(petAccessGuard.canonicalPetUuid("6up32b")).thenReturn("6UP32B");
		when(petFeedingLogDao.findByPetUuidBetween(eq("6UP32B"), any(), any())).thenReturn(List.of());

		controller.feedingLogs("6up32b", null, null);

		verify(petFeedingLogDao).findByPetUuidBetween(eq("6UP32B"), any(LocalDateTime.class), any(LocalDateTime.class));
	}

	@Test
	void getDefaultWindowIncludesMiddayToday() {
		when(petAccessGuard.canonicalPetUuid("9AP1AU")).thenReturn("9AP1AU");
		when(petFeedingLogDao.findByPetUuidBetween(eq("9AP1AU"), any(), any())).thenReturn(List.of());

		controller.feedingLogs("9AP1AU", null, null);

		ArgumentCaptor<LocalDateTime> from = ArgumentCaptor.forClass(LocalDateTime.class);
		ArgumentCaptor<LocalDateTime> to = ArgumentCaptor.forClass(LocalDateTime.class);
		verify(petFeedingLogDao).findByPetUuidBetween(eq("9AP1AU"), from.capture(), to.capture());
		LocalDateTime noon = LocalDate.now().atTime(12, 0);
		assertFalse(to.getValue().isBefore(noon));
	}

	@Test
	void postWithoutDailyPlanIdSavesNullPlanLink() {
		when(petAccessGuard.canonicalPetUuid("6up32b")).thenReturn("6UP32B");
		when(petFeedingLogDao.save(any(PetFeedingLog.class))).thenAnswer(inv -> inv.getArgument(0));

		controller.addFeedingLog("6up32b",
				new PetFeedingLogRequest(null, FeedingStatus.COMPLETED, null, "from dashboard", null));

		ArgumentCaptor<PetFeedingLog> captor = ArgumentCaptor.forClass(PetFeedingLog.class);
		verify(petFeedingLogDao).save(captor.capture());
		assertEquals("6UP32B", captor.getValue().getPetUuid());
		assertNull(captor.getValue().getDailyPlanId());
		assertEquals(FeedingStatus.COMPLETED, captor.getValue().getStatus());
	}
}
