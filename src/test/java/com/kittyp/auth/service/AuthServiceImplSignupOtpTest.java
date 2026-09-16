package com.kittyp.auth.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.kittyp.auth.dto.SignupOtpSendRequest;
import com.kittyp.auth.util.JwtUtils;
import com.kittyp.clinic.dao.ClinicDao;
import com.kittyp.clinic.repository.ClinicDoctorInviteRepository;
import com.kittyp.clinic.repository.ClinicDoctorRepository;
import com.kittyp.clinic.service.ClinicOwnerUserLinkService;
import com.kittyp.common.exception.CustomException;
import com.kittyp.common.util.VerificationCodeService;
import com.kittyp.doctor.dao.DoctorProfileDao;
import com.kittyp.email.service.ZeptoMailService;
import com.kittyp.notification.service.SmsService;
import com.kittyp.user.dao.RoleDao;
import com.kittyp.user.dao.UserDao;
import com.kittyp.user.service.PhoneAvailabilityService;

@ExtendWith(MockitoExtension.class)
class AuthServiceImplSignupOtpTest {

	@Mock
	private UserDao userDao;
	@Mock
	private PasswordEncoder encoder;
	@Mock
	private RoleDao roleDao;
	@Mock
	private AuthenticationManager authenticationManager;
	@Mock
	private JwtUtils jwtUtils;
	@Mock
	private ZeptoMailService zeptoMailService;
	@Mock
	private GoogleOAuth2Service googleOAuth2Service;
	@Mock
	private ClinicDao clinicDao;
	@Mock
	private ClinicDoctorRepository clinicDoctorRepository;
	@Mock
	private ClinicDoctorInviteRepository clinicDoctorInviteRepository;
	@Mock
	private DoctorProfileDao doctorProfileDao;
	@Mock
	private VerificationCodeService verificationCodeService;
	@Mock
	private SmsService smsService;
	@Mock
	private MasterTotpService masterTotpService;
	@Mock
	private ClinicOwnerUserLinkService clinicOwnerUserLinkService;
	@Mock
	private LoginRateLimiter loginRateLimiter;
	@Mock
	private PhoneAvailabilityService phoneAvailabilityService;

	private AuthServiceImpl service;

	@BeforeEach
	void setUp() {
		service = new AuthServiceImpl(userDao, encoder, roleDao, authenticationManager, jwtUtils, zeptoMailService,
				googleOAuth2Service, clinicDao, clinicDoctorRepository, clinicDoctorInviteRepository, doctorProfileDao,
				verificationCodeService, smsService, masterTotpService, clinicOwnerUserLinkService, loginRateLimiter,
				phoneAvailabilityService);
	}

	@Test
	void sendSignupOtp_phoneTaken_doesNotSendSms() {
		doThrow(new CustomException(PhoneAvailabilityService.ALREADY_IN_USE, HttpStatus.CONFLICT))
				.when(phoneAvailabilityService).assertAvailable(eq("7798296970"), isNull());

		SignupOtpSendRequest request = new SignupOtpSendRequest();
		request.setChannel("PHONE");
		request.setPhone("+917798296970");

		CustomException ex = assertThrows(CustomException.class, () -> service.sendSignupOtp(request));
		assertEquals(HttpStatus.CONFLICT, ex.getHttpStatus());
		assertEquals(PhoneAvailabilityService.ALREADY_IN_USE, ex.getMessage());
		verify(smsService, never()).sendOtp(anyString(), anyString(), org.mockito.ArgumentMatchers.any());
	}

	@Test
	void sendSignupOtp_phoneFree_sendsSms() {
		when(verificationCodeService.generateCode(anyString())).thenReturn("123456");

		SignupOtpSendRequest request = new SignupOtpSendRequest();
		request.setChannel("PHONE");
		request.setPhone("+917798296970");

		service.sendSignupOtp(request);

		verify(phoneAvailabilityService).assertAvailable("7798296970", null);
		verify(smsService).sendOtp("+917798296970", "123456", null);
	}
}
