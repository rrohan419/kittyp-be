package com.kittyp.auth.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.atLeast;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.kittyp.clinic.dao.ClinicDao;
import com.kittyp.clinic.entity.Clinic;
import com.kittyp.clinic.entity.ClinicDoctorInvite;
import com.kittyp.clinic.enums.ClinicDoctorInviteStatus;
import com.kittyp.clinic.enums.ClinicStatus;
import com.kittyp.clinic.repository.ClinicDoctorInviteRepository;
import com.kittyp.clinic.repository.ClinicDoctorRepository;
import com.kittyp.clinic.service.ClinicOwnerUserLinkService;
import com.kittyp.common.constants.ResponseMessage;
import com.kittyp.common.dto.PublicSignupRequestDto;
import com.kittyp.common.dto.SignupClinicRequestDto;
import com.kittyp.common.dto.SignupDoctorRequestDto;
import com.kittyp.common.enums.SignupRole;
import com.kittyp.common.exception.CustomException;
import com.kittyp.common.exception.ResourceAlreadyExistsException;
import com.kittyp.common.util.VerificationCodeService;
import com.kittyp.doctor.dao.DoctorProfileDao;
import com.kittyp.email.service.ZeptoMailService;
import com.kittyp.user.dao.RoleDao;
import com.kittyp.user.dao.UserDao;
import com.kittyp.user.entity.Role;
import com.kittyp.user.entity.User;
import com.kittyp.user.enums.ERole;

class AuthServiceImplRegisterTest {

	private UserDao userDao;
	private PasswordEncoder encoder;
	private RoleDao roleDao;
	private ZeptoMailService zeptoMailService;
	private ClinicDao clinicDao;
	private ClinicDoctorRepository clinicDoctorRepository;
	private ClinicDoctorInviteRepository clinicDoctorInviteRepository;
	private DoctorProfileDao doctorProfileDao;
	private VerificationCodeService verificationCodeService;
	private RecordingLinkService clinicOwnerUserLinkService;
	private AuthServiceImpl authService;

	@BeforeEach
	void setUp() {
		userDao = mock(UserDao.class);
		encoder = mock(PasswordEncoder.class);
		roleDao = mock(RoleDao.class);
		zeptoMailService = mock(ZeptoMailService.class);
		clinicDao = mock(ClinicDao.class);
		clinicDoctorRepository = mock(ClinicDoctorRepository.class);
		clinicDoctorInviteRepository = mock(ClinicDoctorInviteRepository.class);
		doctorProfileDao = mock(DoctorProfileDao.class);
		verificationCodeService = new VerificationCodeService();
		clinicOwnerUserLinkService = new RecordingLinkService();

		when(encoder.encode(any())).thenReturn("encoded");
		when(userDao.saveUser(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));
		when(clinicDao.saveClinic(any(Clinic.class))).thenAnswer(invocation -> invocation.getArgument(0));
		when(clinicDao.findAllByOwnerUserId(any())).thenReturn(List.of());

		authService = new AuthServiceImpl(
				userDao,
				encoder,
				roleDao,
				null,
				null,
				zeptoMailService,
				null,
				clinicDao,
				clinicDoctorRepository,
				clinicDoctorInviteRepository,
				doctorProfileDao,
				verificationCodeService,
				null,
				clinicOwnerUserLinkService,
				null);
	}

	@Test
	void register_userRole_assignsRoleUserOnly() {
		PublicSignupRequestDto req = baseRequest();
		req.setRole(SignupRole.USER);
		req.setClinicName("ShouldBeIgnored");
		req.setRegistrationNumber("VET-HACK");
		req.setDegreeCertificateUrl("https://evil.example/cert.pdf");

		when(userDao.userPresentByEmail(req.getEmail())).thenReturn(false);
		when(roleDao.roleByName(ERole.ROLE_USER)).thenReturn(role(ERole.ROLE_USER));

		assertEquals(ResponseMessage.USER_REGISTERED_SUCCESSFULLY, authService.register(req).getMessage());

		verify(roleDao).roleByName(ERole.ROLE_USER);
		verify(roleDao, never()).roleByName(ERole.ROLE_DOCTOR);
		verify(roleDao, never()).roleByName(ERole.ROLE_CLINIC_ADMIN);
		verify(roleDao, never()).roleByName(ERole.ROLE_ADMIN);
		verify(roleDao, never()).roleByName(ERole.ROLE_MODERATOR);
		verify(roleDao, never()).roleByName(ERole.ROLE_CLINIC_STAFF);
		verify(doctorProfileDao, never()).save(any());
		verify(clinicDao, never()).saveClinic(any());
		assertEquals(1, clinicOwnerUserLinkService.calls);
	}

	@Test
	void register_omittedRole_defaultsToUser() {
		PublicSignupRequestDto req = baseRequest();
		req.setRole(null);

		when(userDao.userPresentByEmail(req.getEmail())).thenReturn(false);
		when(roleDao.roleByName(ERole.ROLE_USER)).thenReturn(role(ERole.ROLE_USER));

		authService.register(req);

		verify(roleDao).roleByName(ERole.ROLE_USER);
		verify(doctorProfileDao, never()).save(any());
		verify(clinicDao, never()).saveClinic(any());
	}

	@Test
	void register_legacyRolesArray_cannotEscalateToAdmin() {
		PublicSignupRequestDto req = baseRequest();
		req.setRole(SignupRole.USER);
		req.setRoles(Set.of("ROLE_ADMIN", "ROLE_MODERATOR", "ROLE_CLINIC_STAFF"));

		when(userDao.userPresentByEmail(req.getEmail())).thenReturn(false);
		when(roleDao.roleByName(ERole.ROLE_USER)).thenReturn(role(ERole.ROLE_USER));

		authService.register(req);

		verify(roleDao).roleByName(ERole.ROLE_USER);
		verify(roleDao, never()).roleByName(ERole.ROLE_ADMIN);
		verify(roleDao, never()).roleByName(ERole.ROLE_MODERATOR);
		verify(roleDao, never()).roleByName(ERole.ROLE_CLINIC_STAFF);
	}

	@Test
	void register_duplicateEmail_rejectsWithoutProvisioning() {
		PublicSignupRequestDto req = baseRequest();
		req.setRole(SignupRole.USER);
		when(userDao.userPresentByEmail(req.getEmail())).thenReturn(true);

		assertThrows(ResourceAlreadyExistsException.class, () -> authService.register(req));
		verify(roleDao, never()).roleByName(any());
		verify(userDao, never()).saveUser(any());
	}

	@Test
	void register_doctor_requiresPhone() {
		PublicSignupRequestDto req = doctorRequest();
		req.setPhoneNumber(" ");
		when(userDao.userPresentByEmail(req.getEmail())).thenReturn(false);

		CustomException ex = assertThrows(CustomException.class, () -> authService.register(req));
		assertEquals(HttpStatus.BAD_REQUEST, ex.getHttpStatus());
		verify(roleDao, never()).roleByName(ERole.ROLE_DOCTOR);
	}

	@Test
	void register_doctor_requiresRegistrationNumber() {
		PublicSignupRequestDto req = doctorRequest();
		req.setRegistrationNumber(" ");
		when(userDao.userPresentByEmail(req.getEmail())).thenReturn(false);

		assertThrows(CustomException.class, () -> authService.register(req));
		verify(doctorProfileDao, never()).save(any());
	}

	@Test
	void register_doctor_requiresCertificatesAndOtp() {
		PublicSignupRequestDto req = baseRequest();
		req.setRole(SignupRole.DOCTOR);
		req.setPhoneNumber("9876543210");
		req.setRegistrationNumber("VET-1");

		when(userDao.userPresentByEmail(req.getEmail())).thenReturn(false);

		CustomException ex = assertThrows(CustomException.class, () -> authService.register(req));
		assertEquals(HttpStatus.BAD_REQUEST, ex.getHttpStatus());
		verify(roleDao, never()).roleByName(ERole.ROLE_DOCTOR);
		verify(doctorProfileDao, never()).save(any());
	}

	@Test
	void register_doctor_requiresEmailOtp() {
		PublicSignupRequestDto req = doctorRequest();
		when(userDao.userPresentByEmail(req.getEmail())).thenReturn(false);
		verificationCodeService.markVerified(VerificationCodeService.phoneVerifiedKey(req.getPhoneNumber()));

		CustomException ex = assertThrows(CustomException.class, () -> authService.register(req));
		assertEquals("Email OTP verification required", ex.getMessage());
		verify(roleDao, never()).roleByName(ERole.ROLE_DOCTOR);
	}

	@Test
	void register_doctor_requiresPhoneOtp() {
		PublicSignupRequestDto req = doctorRequest();
		when(userDao.userPresentByEmail(req.getEmail())).thenReturn(false);
		verificationCodeService.markVerified(VerificationCodeService.emailVerifiedKey(req.getEmail()));

		CustomException ex = assertThrows(CustomException.class, () -> authService.register(req));
		assertEquals("Phone OTP verification required", ex.getMessage());
		verify(roleDao, never()).roleByName(ERole.ROLE_DOCTOR);
	}

	@Test
	void register_doctor_acceptsPlus91PhoneOtpKey() {
		PublicSignupRequestDto req = doctorRequest();
		when(userDao.userPresentByEmail(req.getEmail())).thenReturn(false);
		when(roleDao.roleByName(ERole.ROLE_DOCTOR)).thenReturn(role(ERole.ROLE_DOCTOR));
		verificationCodeService.markVerified(VerificationCodeService.emailVerifiedKey(req.getEmail()));
		verificationCodeService.markVerified(VerificationCodeService.phoneVerifiedKey("+91" + req.getPhoneNumber()));
		when(doctorProfileDao.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

		authService.register(req);

		verify(roleDao).roleByName(ERole.ROLE_DOCTOR);
		verify(doctorProfileDao, atLeastOnce()).save(any());
		verify(clinicDao).saveClinic(argThat(clinic -> clinic.getStatus() == ClinicStatus.VERIFIED));
	}

	@Test
	void register_doctor_assignsRoleDoctorAndSavesProfile() {
		PublicSignupRequestDto req = doctorRequest();
		stubDoctorReady(req);

		authService.register(req);

		verify(roleDao).roleByName(ERole.ROLE_DOCTOR);
		verify(roleDao, never()).roleByName(ERole.ROLE_USER);
		verify(roleDao, never()).roleByName(ERole.ROLE_ADMIN);
		verify(doctorProfileDao, atLeastOnce()).save(any());
		verify(clinicDao).saveClinic(argThat(clinic -> clinic.getStatus() == ClinicStatus.VERIFIED
				&& clinic.getName() != null && clinic.getName().startsWith("Dr. ")));
		verify(clinicDoctorRepository).save(any());
		assertEquals(0, clinicOwnerUserLinkService.calls);
	}

	@Test
	void register_doctor_withClinicName_doesNotCreateClinic() {
		PublicSignupRequestDto req = doctorRequest();
		req.setClinicName("Happy Paws");
		req.setClinicAddress("1 Main St");
		stubDoctorReady(req);

		authService.register(req);

		verify(clinicDao).saveClinic(argThat(clinic -> !"Happy Paws".equals(clinic.getName())
				&& clinic.getStatus() == ClinicStatus.VERIFIED));
		verify(clinicDoctorRepository).save(any());
	}

	@Test
	void register_doctor_invalidInviteToken_rejected() {
		PublicSignupRequestDto req = doctorRequest();
		req.setInviteToken("nope");
		stubDoctorReady(req);
		when(clinicDoctorInviteRepository.findByToken("nope")).thenReturn(Optional.empty());

		CustomException ex = assertThrows(CustomException.class, () -> authService.register(req));
		assertEquals(HttpStatus.BAD_REQUEST, ex.getHttpStatus());
		verify(doctorProfileDao, never()).save(any());
	}

	@Test
	void register_doctor_expiredInvite_rejected() {
		PublicSignupRequestDto req = doctorRequest();
		req.setInviteToken("tok");
		stubDoctorReady(req);
		when(clinicDoctorInviteRepository.findByToken("tok")).thenReturn(Optional.of(invite("tok",
				req.getEmail(), ClinicDoctorInviteStatus.PENDING, LocalDateTime.now().minusHours(1))));

		assertThrows(CustomException.class, () -> authService.register(req));
		verify(doctorProfileDao, never()).save(any());
	}

	@Test
	void register_doctor_inviteEmailMismatch_rejected() {
		PublicSignupRequestDto req = doctorRequest();
		req.setInviteToken("tok");
		stubDoctorReady(req);
		when(clinicDoctorInviteRepository.findByToken("tok")).thenReturn(Optional.of(invite("tok",
				"other@example.com", ClinicDoctorInviteStatus.PENDING, LocalDateTime.now().plusDays(1))));

		assertThrows(CustomException.class, () -> authService.register(req));
		verify(doctorProfileDao, never()).save(any());
	}

	@Test
	void register_doctor_validInvite_joinsClinicAndCreatesPersonalPractice() {
		PublicSignupRequestDto req = doctorRequest();
		req.setInviteToken("tok");
		req.setClinicName("ShouldNotCreate");
		stubDoctorReady(req);
		ClinicDoctorInvite invite = invite("tok", req.getEmail(), ClinicDoctorInviteStatus.PENDING,
				LocalDateTime.now().plusDays(1));
		when(clinicDoctorInviteRepository.findByToken("tok")).thenReturn(Optional.of(invite));
		when(doctorProfileDao.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

		authService.register(req);

		verify(clinicDao).saveClinic(argThat(clinic -> clinic.getStatus() == ClinicStatus.VERIFIED));
		verify(clinicDoctorRepository, atLeast(2)).save(any());
		assertEquals(ClinicDoctorInviteStatus.ACCEPTED, invite.getStatus());
		verify(clinicDoctorInviteRepository).save(invite);
	}

	@Test
	void register_clinic_requiresEmailOtp() {
		PublicSignupRequestDto req = clinicRequest();
		when(userDao.userPresentByEmail(req.getEmail())).thenReturn(false);

		CustomException ex = assertThrows(CustomException.class, () -> authService.register(req));
		assertEquals(HttpStatus.BAD_REQUEST, ex.getHttpStatus());
		verify(roleDao, never()).roleByName(ERole.ROLE_CLINIC_ADMIN);
		verify(clinicDao, never()).saveClinic(any());
	}

	@Test
	void register_clinic_requiresClinicName() {
		PublicSignupRequestDto req = clinicRequest();
		req.setClinicName("  ");
		when(userDao.userPresentByEmail(req.getEmail())).thenReturn(false);
		verificationCodeService.markVerified(VerificationCodeService.emailVerifiedKey(req.getEmail()));

		CustomException ex = assertThrows(CustomException.class, () -> authService.register(req));
		assertEquals("Clinic name is required", ex.getMessage());
		verify(roleDao, never()).roleByName(ERole.ROLE_CLINIC_ADMIN);
	}

	@Test
	void register_clinic_assignsClinicAdminAndSavesClinic() {
		PublicSignupRequestDto req = clinicRequest();
		when(userDao.userPresentByEmail(req.getEmail())).thenReturn(false);
		when(roleDao.roleByName(ERole.ROLE_CLINIC_ADMIN)).thenReturn(role(ERole.ROLE_CLINIC_ADMIN));
		verificationCodeService.markVerified(VerificationCodeService.emailVerifiedKey(req.getEmail()));
		when(clinicDao.saveClinic(any())).thenAnswer(invocation -> invocation.getArgument(0));

		authService.register(req);

		verify(roleDao).roleByName(ERole.ROLE_CLINIC_ADMIN);
		verify(roleDao, never()).roleByName(ERole.ROLE_ADMIN);
		verify(roleDao, never()).roleByName(ERole.ROLE_CLINIC_STAFF);
		verify(clinicDao).saveClinic(argThat(clinic -> clinic.getStatus() == ClinicStatus.PENDING
				&& "Paws Clinic".equals(clinic.getName())));
		verify(doctorProfileDao, never()).save(any());
	}

	@Test
	void dedicatedRegisterDoctor_stillProvisionsDoctor() {
		SignupDoctorRequestDto req = new SignupDoctorRequestDto();
		req.setFirstName("Ada");
		req.setEmail("ada@example.com");
		req.setPassword("Passw0rd!");
		req.setPhoneNumber("9876543210");
		req.setRegistrationNumber("VET-1");
		req.setDegreeCertificateUrl("https://files.example/degree.pdf");
		req.setRegistrationCertificateUrl("https://files.example/reg.pdf");

		when(userDao.userPresentByEmail(req.getEmail())).thenReturn(false);
		when(roleDao.roleByName(ERole.ROLE_DOCTOR)).thenReturn(role(ERole.ROLE_DOCTOR));
		verificationCodeService.markVerified(VerificationCodeService.emailVerifiedKey(req.getEmail()));
		verificationCodeService.markVerified(VerificationCodeService.phoneVerifiedKey(req.getPhoneNumber()));
		when(doctorProfileDao.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

		authService.registerDoctor(req);

		verify(roleDao).roleByName(ERole.ROLE_DOCTOR);
	}

	@Test
	void dedicatedRegisterClinic_stillProvisionsClinicAdmin() {
		SignupClinicRequestDto req = new SignupClinicRequestDto();
		req.setFirstName("Ada");
		req.setEmail("ada@example.com");
		req.setPassword("Passw0rd!");
		req.setClinicName("Paws Clinic");

		when(userDao.userPresentByEmail(req.getEmail())).thenReturn(false);
		when(roleDao.roleByName(ERole.ROLE_CLINIC_ADMIN)).thenReturn(role(ERole.ROLE_CLINIC_ADMIN));
		verificationCodeService.markVerified(VerificationCodeService.emailVerifiedKey(req.getEmail()));
		when(clinicDao.saveClinic(any())).thenAnswer(invocation -> invocation.getArgument(0));

		authService.registerClinic(req);

		verify(roleDao).roleByName(ERole.ROLE_CLINIC_ADMIN);
	}

	private void stubDoctorReady(PublicSignupRequestDto req) {
		when(userDao.userPresentByEmail(req.getEmail())).thenReturn(false);
		when(roleDao.roleByName(ERole.ROLE_DOCTOR)).thenReturn(role(ERole.ROLE_DOCTOR));
		verificationCodeService.markVerified(VerificationCodeService.emailVerifiedKey(req.getEmail()));
		verificationCodeService.markVerified(VerificationCodeService.phoneVerifiedKey(req.getPhoneNumber()));
		when(doctorProfileDao.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
	}

	private static PublicSignupRequestDto baseRequest() {
		PublicSignupRequestDto req = new PublicSignupRequestDto();
		req.setFirstName("Ada");
		req.setLastName("Lovelace");
		req.setEmail("ada@example.com");
		req.setPassword("Passw0rd!");
		return req;
	}

	private static PublicSignupRequestDto doctorRequest() {
		PublicSignupRequestDto req = baseRequest();
		req.setRole(SignupRole.DOCTOR);
		req.setPhoneNumber("9876543210");
		req.setRegistrationNumber("VET-1");
		req.setDegreeCertificateUrl("https://files.example/degree.pdf");
		req.setRegistrationCertificateUrl("https://files.example/reg.pdf");
		return req;
	}

	private static PublicSignupRequestDto clinicRequest() {
		PublicSignupRequestDto req = baseRequest();
		req.setRole(SignupRole.CLINIC);
		req.setClinicName("Paws Clinic");
		return req;
	}

	private static ClinicDoctorInvite invite(String token, String email, ClinicDoctorInviteStatus status,
			LocalDateTime expiresAt) {
		return ClinicDoctorInvite.builder()
				.token(token)
				.email(email)
				.doctorName("Dr Ada")
				.status(status)
				.expiresAt(expiresAt)
				.clinic(Clinic.builder().name("Host Clinic").build())
				.invitedByUserId(1L)
				.build();
	}

	private static Role role(ERole name) {
		Role role = new Role();
		role.setName(name);
		return role;
	}

	private static final class RecordingLinkService extends ClinicOwnerUserLinkService {
		private int calls;

		private RecordingLinkService() {
			super(null, null, null, null);
		}

		@Override
		public int linkUserToClinicOwners(User user) {
			calls++;
			return 0;
		}
	}
}
