package com.kittyp.auth.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.transaction.annotation.Transactional;

import com.kittyp.auth.dto.ActivateRoleRequest;
import com.kittyp.clinic.dao.ClinicDao;
import com.kittyp.clinic.entity.Clinic;
import com.kittyp.clinic.enums.ClinicStatus;
import com.kittyp.clinic.service.ClinicOwnerUserLinkService;
import com.kittyp.common.enums.SignupRole;
import com.kittyp.common.exception.CustomException;
import com.kittyp.common.util.VerificationCodeService;
import com.kittyp.doctor.dao.DoctorProfileDao;
import com.kittyp.doctor.entity.DoctorProfile;
import com.kittyp.user.dao.RoleDao;
import com.kittyp.user.dao.UserDao;
import com.kittyp.user.entity.Role;
import com.kittyp.user.entity.User;
import com.kittyp.user.entity.UserRole;
import com.kittyp.user.enums.ERole;

class RoleActivationServiceTest {

	private UserDao userDao;
	private RoleDao roleDao;
	private DoctorProfileDao doctorProfileDao;
	private ClinicDao clinicDao;
	private VerificationCodeService verificationCodeService;
	private ClinicOwnerUserLinkService clinicOwnerUserLinkService;
	private AuthServiceImpl authService;
	private RoleCredentialService roleCredentialService;
	private RoleActivationService service;

	@BeforeEach
	void setUp() {
		userDao = org.mockito.Mockito.mock(UserDao.class);
		roleDao = org.mockito.Mockito.mock(RoleDao.class);
		doctorProfileDao = org.mockito.Mockito.mock(DoctorProfileDao.class);
		clinicDao = org.mockito.Mockito.mock(ClinicDao.class);
		verificationCodeService = new VerificationCodeService();
		clinicOwnerUserLinkService = org.mockito.Mockito.mock(ClinicOwnerUserLinkService.class);
		authService = org.mockito.Mockito.mock(AuthServiceImpl.class);
		roleCredentialService = org.mockito.Mockito.mock(RoleCredentialService.class);
		when(userDao.saveUser(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));
		when(clinicDao.saveClinic(any(Clinic.class))).thenAnswer(invocation -> invocation.getArgument(0));
		when(roleDao.roleByName(any())).thenAnswer(invocation -> {
			Role role = new Role();
			role.setName(invocation.getArgument(0));
			return role;
		});
		service = new RoleActivationService(userDao, roleDao, doctorProfileDao, clinicDao, verificationCodeService,
				clinicOwnerUserLinkService, authService, roleCredentialService);
	}

	@Test
	void activate_isTransactional() throws Exception {
		Transactional transactional = RoleActivationService.class
				.getMethod("activate", String.class, ActivateRoleRequest.class)
				.getAnnotation(Transactional.class);
		assertTrue(transactional != null);
	}

	@Test
	void doctor_activatesParentOnce() {
		User user = userWith(ERole.ROLE_DOCTOR, "encoded-secret");
		stubSession(user);

		ActivateRoleRequest request = new ActivateRoleRequest();
		request.setRole(SignupRole.USER);

		RoleActivationResult first = service.activate(user.getEmail(), request);
		RoleActivationResult second = service.activate(user.getEmail(), request);

		assertEquals(RoleActivationService.PARENT_ADDED, first.message());
		assertEquals("This role is already on your account.", second.message());
		assertTrue(hasRole(user, ERole.ROLE_USER));
		assertTrue(hasRole(user, ERole.ROLE_DOCTOR));
		assertEquals("encoded-secret", user.getPassword());
		verify(userDao, times(1)).saveUser(user);
		verify(clinicOwnerUserLinkService, times(1)).linkUserToClinicOwners(user);
		verify(authService, never()).provisionNewDoctor(any(), any());
	}

	@Test
	void parentActivation_linkFailure_doesNotReturnSuccess() {
		User user = userWith(ERole.ROLE_DOCTOR, "encoded-secret");
		stubSession(user);
		when(clinicOwnerUserLinkService.linkUserToClinicOwners(user))
				.thenThrow(new IllegalStateException("link failed"));

		ActivateRoleRequest request = new ActivateRoleRequest();
		request.setRole(SignupRole.USER);

		assertThrows(IllegalStateException.class, () -> service.activate(user.getEmail(), request));
	}

	@Test
	void parent_activatesDoctor_reusesSignupValidation() {
		User user = userWith(ERole.ROLE_USER, "encoded-secret");
		user.setPhoneNumber("1112223333");
		stubSession(user);
		when(doctorProfileDao.findByUserId(user.getId())).thenReturn(null);

		ActivateRoleRequest request = doctorRequest();
		CustomException missingOtp = assertThrows(CustomException.class,
				() -> service.activate(user.getEmail(), request));
		assertEquals("Phone OTP verification required", missingOtp.getMessage());
		verify(authService, never()).provisionNewDoctor(any(), any());

		verificationCodeService.markVerified(VerificationCodeService.phoneVerifiedKey(request.getPhoneNumber()));
		RoleActivationResult result = service.activate(user.getEmail(), request);

		assertEquals(RoleActivationService.DOCTOR_ADDED, result.message());
		assertEquals("encoded-secret", user.getPassword());
		assertEquals("1112223333", user.getPhoneNumber());
		assertTrue(hasRole(user, ERole.ROLE_DOCTOR));
		assertTrue(hasRole(user, ERole.ROLE_USER));
		verify(authService, times(1)).provisionNewDoctor(any(), any());
		verify(doctorProfileDao, never()).save(any());
	}

	@Test
	void secondDoctorActivation_doesNotCreateAnotherProfile() {
		User user = userWith(ERole.ROLE_DOCTOR, "encoded-secret");
		stubSession(user);
		when(doctorProfileDao.findByUserId(user.getId())).thenReturn(new DoctorProfile());

		ActivateRoleRequest request = doctorRequest();
		RoleActivationResult result = service.activate(user.getEmail(), request);

		assertEquals("This role is already on your account.", result.message());
		verify(authService, never()).provisionNewDoctor(any(), any());
		verify(doctorProfileDao, never()).save(any());
	}

	@Test
	void doctor_activatesClinicOnce() {
		User user = userWith(ERole.ROLE_DOCTOR, "encoded-secret");
		stubSession(user);

		ActivateRoleRequest request = new ActivateRoleRequest();
		request.setRole(SignupRole.CLINIC);
		request.setClinicName("Paws Hospital");

		RoleActivationResult first = service.activate(user.getEmail(), request);
		RoleActivationResult second = service.activate(user.getEmail(), request);

		assertEquals(RoleActivationService.CLINIC_ADDED, first.message());
		assertEquals("This role is already on your account.", second.message());
		assertTrue(hasRole(user, ERole.ROLE_DOCTOR));
		assertTrue(hasRole(user, ERole.ROLE_CLINIC_ADMIN));
		verify(clinicDao, times(1)).saveClinic(argThat(clinic -> clinic.getStatus() == ClinicStatus.PENDING
				&& clinic.getOwner() == user
				&& "Paws Hospital".equals(clinic.getName())));
	}

	@Test
	void activate_rejectsMismatchedEmail() {
		User user = userWith(ERole.ROLE_USER, "encoded-secret");
		stubSession(user);
		ActivateRoleRequest request = new ActivateRoleRequest();
		request.setRole(SignupRole.DOCTOR);
		request.setEmail("other@example.com");

		CustomException ex = assertThrows(CustomException.class, () -> service.activate(user.getEmail(), request));
		assertEquals(HttpStatus.FORBIDDEN, ex.getHttpStatus());
		verify(userDao, never()).lockById(any());
	}

	@Test
	void assertSelfService_rejectsAdmin() {
		CustomException ex = assertThrows(CustomException.class,
				() -> RoleActivationService.assertSelfService(ERole.ROLE_ADMIN));
		assertEquals(HttpStatus.FORBIDDEN, ex.getHttpStatus());
	}

	@Test
	void assertSelfService_rejectsModeratorAndClinicStaff() {
		CustomException moderator = assertThrows(CustomException.class,
				() -> RoleActivationService.assertSelfService(ERole.ROLE_MODERATOR));
		CustomException staff = assertThrows(CustomException.class,
				() -> RoleActivationService.assertSelfService(ERole.ROLE_CLINIC_STAFF));
		assertEquals(HttpStatus.FORBIDDEN, moderator.getHttpStatus());
		assertEquals(HttpStatus.FORBIDDEN, staff.getHttpStatus());
	}

	@Test
	void activate_blankOrMissingSessionEmail_doesNotPersist() {
		ActivateRoleRequest request = new ActivateRoleRequest();
		request.setRole(SignupRole.USER);

		CustomException blank = assertThrows(CustomException.class, () -> service.activate("  ", request));
		CustomException missing = assertThrows(CustomException.class, () -> service.activate(null, request));

		assertEquals(HttpStatus.UNAUTHORIZED, blank.getHttpStatus());
		assertEquals(HttpStatus.UNAUTHORIZED, missing.getHttpStatus());
		verify(userDao, never()).saveUser(any());
		verify(userDao, never()).lockById(any());
		verify(clinicDao, never()).saveClinic(any());
		verify(authService, never()).provisionNewDoctor(any(), any());
	}

	@Test
	void existingDoctor_doesNotStoreAnotherPassword() {
		User user = userWith(ERole.ROLE_DOCTOR, "encoded-secret");
		stubSession(user);
		when(doctorProfileDao.findByUserId(user.getId())).thenReturn(new DoctorProfile());
		ActivateRoleRequest request = doctorRequest();
		request.setRolePassword("OtherSecret1!");

		service.activate(user.getEmail(), request);

		verify(roleCredentialService, never()).storeNewRolePassword(any(), any(), any());
		verify(authService, never()).provisionNewDoctor(any(), any());
	}

	@Test
	void newParent_storesRolePasswordWithoutChangingAccountPassword() {
		User user = userWith(ERole.ROLE_DOCTOR, "encoded-secret");
		stubSession(user);
		ActivateRoleRequest request = new ActivateRoleRequest();
		request.setRole(SignupRole.USER);
		request.setRolePassword("ParentPass1!");

		service.activate(user.getEmail(), request);

		assertEquals("encoded-secret", user.getPassword());
		verify(roleCredentialService).storeNewRolePassword(user, ERole.ROLE_USER, "ParentPass1!");
	}

	private void stubSession(User user) {
		when(userDao.userByEmail(user.getEmail())).thenReturn(user);
		when(userDao.lockById(user.getId())).thenReturn(user);
	}

	private static ActivateRoleRequest doctorRequest() {
		ActivateRoleRequest request = new ActivateRoleRequest();
		request.setRole(SignupRole.DOCTOR);
		request.setPhoneNumber("9876543210");
		request.setRegistrationNumber("VET-1");
		request.setDegreeCertificateUrl("https://files.example/degree.pdf");
		request.setRegistrationCertificateUrl("https://files.example/reg.pdf");
		return request;
	}

	private static User userWith(ERole roleName, String password) {
		User user = User.builder()
				.email("ada@example.com")
				.password(password)
				.firstName("Ada")
				.build();
		user.setId(7L);
		Role role = new Role();
		role.setName(roleName);
		user.addRole(role);
		return user;
	}

	private static boolean hasRole(User user, ERole role) {
		for (UserRole userRole : user.getUserRoles()) {
			if (userRole.getRole() != null && role == userRole.getRole().getName()) {
				return true;
			}
		}
		return false;
	}
}
