package com.kittyp.clinic.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.HashSet;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;

import com.kittyp.clinic.dao.ClinicDao;
import com.kittyp.clinic.dto.ClinicDtos.DoctorInviteRequest;
import com.kittyp.common.exception.ResourceNotFoundException;
import com.kittyp.user.dao.UserDao;
import com.kittyp.user.entity.Role;
import com.kittyp.user.entity.User;
import com.kittyp.user.entity.UserRole;
import com.kittyp.user.enums.ERole;

@ExtendWith(MockitoExtension.class)
class ClinicServiceImplInviteAuthTest {

	@Mock
	private UserDao userDao;

	@Mock
	private ClinicDao clinicDao;

	@InjectMocks
	private ClinicServiceImpl clinicService;

	@Test
	void inviteDoctor_doctorOnly_throwsAccessDenied() {
		when(userDao.userByEmail("doc@example.com")).thenReturn(userWithRoles(2L, "doc@example.com", ERole.ROLE_DOCTOR));

		AccessDeniedException ex = assertThrows(AccessDeniedException.class,
				() -> clinicService.inviteDoctor("clinic-uuid",
						new DoctorInviteRequest("Dr A", "a@example.com", null), "doc@example.com"));

		assertEquals("Only clinic admins can invite doctors", ex.getMessage());
		verify(clinicDao, never()).findByUuid(anyString());
	}

	@Test
	void inviteDoctor_clinicAdmin_passesInviteRoleGate() {
		when(userDao.userByEmail("clinic@example.com"))
				.thenReturn(userWithRoles(1L, "clinic@example.com", ERole.ROLE_CLINIC_ADMIN));
		when(clinicDao.findByUuid("clinic-uuid")).thenReturn(null);

		assertThrows(ResourceNotFoundException.class,
				() -> clinicService.inviteDoctor("clinic-uuid",
						new DoctorInviteRequest("Dr A", "a@example.com", null), "clinic@example.com"));
		verify(clinicDao).findByUuid("clinic-uuid");
	}

	@Test
	void inviteDoctor_dualRoleDoctorAndClinicAdmin_passesInviteRoleGate() {
		when(userDao.userByEmail("both@example.com"))
				.thenReturn(userWithRoles(3L, "both@example.com", ERole.ROLE_DOCTOR, ERole.ROLE_CLINIC_ADMIN));
		when(clinicDao.findByUuid("clinic-uuid")).thenReturn(null);

		assertThrows(ResourceNotFoundException.class,
				() -> clinicService.inviteDoctor("clinic-uuid",
						new DoctorInviteRequest("Dr A", "a@example.com", null), "both@example.com"));
		verify(clinicDao).findByUuid("clinic-uuid");
	}

	@Test
	void inviteDoctor_clinicStaff_throwsAccessDenied() {
		when(userDao.userByEmail("staff@example.com"))
				.thenReturn(userWithRoles(4L, "staff@example.com", ERole.ROLE_CLINIC_STAFF));

		AccessDeniedException ex = assertThrows(AccessDeniedException.class,
				() -> clinicService.inviteDoctor("clinic-uuid",
						new DoctorInviteRequest("Dr A", "a@example.com", null), "staff@example.com"));

		assertEquals("Only clinic admins can invite doctors", ex.getMessage());
		verify(clinicDao, never()).findByUuid(anyString());
	}

	private static User userWithRoles(Long id, String email, ERole... roles) {
		User user = User.builder().email(email).password("x").uuid("u-" + id).build();
		user.setId(id);
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
