package com.kittyp.clinic.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

import com.kittyp.clinic.entity.Clinic;
import com.kittyp.clinic.entity.ClinicDoctorInvite;
import com.kittyp.clinic.enums.ClinicDoctorInviteStatus;
import com.kittyp.clinic.enums.ClinicStatus;
import com.kittyp.clinic.repository.ClinicDoctorInviteRepository;
import com.kittyp.common.exception.CustomException;
import com.kittyp.common.exception.ResourceNotFoundException;
import com.kittyp.email.service.ZeptoMailService;
import com.kittyp.notification.repository.NotificationLogRepository;
import com.kittyp.user.dao.UserDao;
import com.kittyp.user.entity.Role;
import com.kittyp.user.entity.User;
import com.kittyp.user.entity.UserRole;
import com.kittyp.user.enums.ERole;

@ExtendWith(MockitoExtension.class)
class ClinicServiceImplDoctorInviteTest {

	@Mock
	private ClinicDoctorInviteRepository clinicDoctorInviteRepository;
	@Mock
	private NotificationLogRepository notificationLogRepository;
	@Mock
	private ZeptoMailService zeptoMailService;
	@Mock
	private UserDao userDao;

	@InjectMocks
	private ClinicServiceImpl clinicService;

	@Test
	void rejectInvite_pending_becomesRejected() {
		ClinicDoctorInvite invite = doctorInvite("t", ClinicDoctorInviteStatus.PENDING,
				LocalDateTime.now().plusDays(1));
		when(clinicDoctorInviteRepository.findByToken("t")).thenReturn(Optional.of(invite));
		when(clinicDoctorInviteRepository.save(any(ClinicDoctorInvite.class))).thenAnswer(i -> i.getArgument(0));

		clinicService.rejectInvite("t");

		assertEquals(ClinicDoctorInviteStatus.REJECTED, invite.getStatus());
		verify(clinicDoctorInviteRepository).save(invite);
		verify(zeptoMailService).sendClinicDoctorInviteResponseEmail(
				anyString(), anyString(), anyString(), anyString(), anyBoolean());
	}

	@Test
	void rejectInvite_expiredPending_marksExpiredAndBadRequest() {
		ClinicDoctorInvite invite = doctorInvite("t", ClinicDoctorInviteStatus.PENDING,
				LocalDateTime.now().minusHours(1));
		when(clinicDoctorInviteRepository.findByToken("t")).thenReturn(Optional.of(invite));
		when(clinicDoctorInviteRepository.save(any(ClinicDoctorInvite.class))).thenAnswer(i -> i.getArgument(0));

		CustomException ex = assertThrows(CustomException.class, () -> clinicService.rejectInvite("t"));

		assertEquals(HttpStatus.BAD_REQUEST, ex.getHttpStatus());
		assertEquals("This invite has expired", ex.getMessage());
		assertEquals(ClinicDoctorInviteStatus.EXPIRED, invite.getStatus());
		verify(zeptoMailService, never()).sendClinicDoctorInviteResponseEmail(
				anyString(), anyString(), anyString(), anyString(), anyBoolean());
	}

	@ParameterizedTest
	@EnumSource(value = ClinicDoctorInviteStatus.class, names = { "ACCEPTED", "REJECTED", "REVOKED", "EXPIRED" })
	void rejectInvite_nonPending_badRequest(ClinicDoctorInviteStatus status) {
		ClinicDoctorInvite invite = doctorInvite("t", status, LocalDateTime.now().plusDays(1));
		when(clinicDoctorInviteRepository.findByToken("t")).thenReturn(Optional.of(invite));

		CustomException ex = assertThrows(CustomException.class, () -> clinicService.rejectInvite("t"));

		assertEquals(HttpStatus.BAD_REQUEST, ex.getHttpStatus());
		assertEquals("This invite is no longer valid", ex.getMessage());
		assertEquals(status, invite.getStatus());
		verify(clinicDoctorInviteRepository, never()).save(any());
	}

	@Test
	void rejectInvite_unknownToken_notFound() {
		when(clinicDoctorInviteRepository.findByToken("missing")).thenReturn(Optional.empty());

		assertThrows(ResourceNotFoundException.class, () -> clinicService.rejectInvite("missing"));
		verify(clinicDoctorInviteRepository, never()).save(any());
	}

	@ParameterizedTest
	@EnumSource(value = ClinicDoctorInviteStatus.class, names = { "REJECTED", "REVOKED" })
	void acceptInvite_afterRejectOrRevoke_badRequest(ClinicDoctorInviteStatus status) {
		ClinicDoctorInvite invite = doctorInvite("t", status, LocalDateTime.now().plusDays(1));
		when(clinicDoctorInviteRepository.findByToken("t")).thenReturn(Optional.of(invite));

		CustomException ex = assertThrows(CustomException.class,
				() -> clinicService.acceptInvite("t", "doc@example.com"));

		assertEquals(HttpStatus.BAD_REQUEST, ex.getHttpStatus());
		assertEquals("This invite is no longer valid", ex.getMessage());
		assertEquals(status, invite.getStatus());
	}

	private static ClinicDoctorInvite doctorInvite(String token, ClinicDoctorInviteStatus status,
			LocalDateTime expiresAt) {
		User owner = userWithRoles(1L, "owner@example.com", ERole.ROLE_CLINIC_ADMIN);
		Clinic clinic = Clinic.builder()
				.uuid("clinic-1")
				.name("Happy Paws")
				.email("clinic@example.com")
				.status(ClinicStatus.VERIFIED)
				.owner(owner)
				.build();
		clinic.setId(10L);
		return ClinicDoctorInvite.builder()
				.clinic(clinic)
				.email("doc@example.com")
				.doctorName("Doc")
				.token(token)
				.status(status)
				.expiresAt(expiresAt)
				.build();
	}

	private static User userWithRoles(Long id, String email, ERole... roles) {
		User user = User.builder().email(email).password("x").uuid("u-" + id)
				.firstName("First").lastName("Last").enabled(true).build();
		user.setId(id);
		user.setIsActive(true);
		Set<UserRole> userRoles = new HashSet<>();
		for (ERole eRole : roles) {
			Role role = new Role();
			role.setName(eRole);
			userRoles.add(new UserRole(user, role));
		}
		user.setUserRoles(userRoles);
		return user;
	}
}
