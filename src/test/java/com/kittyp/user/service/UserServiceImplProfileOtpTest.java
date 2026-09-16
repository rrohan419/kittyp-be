package com.kittyp.user.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
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
import org.springframework.security.crypto.password.PasswordEncoder;

import com.kittyp.auth.service.MasterTotpService;
import com.kittyp.auth.util.JwtUtils;
import com.kittyp.clinic.service.ClinicOwnerUserLinkService;
import com.kittyp.common.exception.CustomException;
import com.kittyp.common.util.VerificationCodeService;
import com.kittyp.email.service.ZeptoMailService;
import com.kittyp.notification.FcmPushNotificationService;
import com.kittyp.notification.service.SmsService;
import com.kittyp.user.dao.RoleDao;
import com.kittyp.user.dao.UserDao;
import com.kittyp.user.dao.UserFcmTokenDao;
import com.kittyp.user.dto.ProfileOtpSendRequest;
import com.kittyp.user.entity.User;

@ExtendWith(MockitoExtension.class)
class UserServiceImplProfileOtpTest {

	@Mock
	private UserDao userDao;
	@Mock
	private RoleDao roleDao;
	@Mock
	private VerificationCodeService verificationCodeService;
	@Mock
	private PasswordEncoder encoder;
	@Mock
	private ZeptoMailService zeptoMailService;
	@Mock
	private FcmPushNotificationService fcmPushNotificationService;
	@Mock
	private UserFcmTokenDao fcmTokenDao;
	@Mock
	private JwtUtils jwtUtils;
	@Mock
	private SmsService smsService;
	@Mock
	private MasterTotpService masterTotpService;
	@Mock
	private ClinicOwnerUserLinkService clinicOwnerUserLinkService;
	@Mock
	private PhoneAvailabilityService phoneAvailabilityService;

	private UserServiceImpl service;

	@BeforeEach
	void setUp() {
		service = new UserServiceImpl(userDao, roleDao, verificationCodeService, encoder, zeptoMailService,
				fcmPushNotificationService, fcmTokenDao, jwtUtils, smsService, masterTotpService,
				clinicOwnerUserLinkService, phoneAvailabilityService);
	}

	@Test
	void sendProfileOtp_phoneTakenByOtherUser_doesNotSendSms() {
		User current = User.builder().uuid("me").email("me@kittyp.test").password("x")
				.phoneCountryCode("+91").phoneNumber("1111111111").build();
		when(userDao.userByEmail("me@kittyp.test")).thenReturn(current);
		doThrow(new CustomException(PhoneAvailabilityService.ALREADY_IN_USE, HttpStatus.CONFLICT))
				.when(phoneAvailabilityService).assertAvailable(eq("9876543210"), eq(current));

		ProfileOtpSendRequest request = new ProfileOtpSendRequest();
		request.setChannel("PHONE");
		request.setPhone("+919876543210");

		CustomException ex = assertThrows(CustomException.class,
				() -> service.sendProfileOtp("me@kittyp.test", request));
		assertEquals(HttpStatus.CONFLICT, ex.getHttpStatus());
		assertEquals("Phone number is already in use", ex.getMessage());
		verify(smsService, never()).sendOtp(anyString(), anyString(), org.mockito.ArgumentMatchers.any());
	}

	@Test
	void sendProfileOtp_phoneFree_sendsSms() {
		User current = User.builder().uuid("me").email("me@kittyp.test").password("x")
				.phoneCountryCode("+91").phoneNumber("1111111111").build();
		when(userDao.userByEmail("me@kittyp.test")).thenReturn(current);
		when(verificationCodeService.generateCode(anyString())).thenReturn("123456");

		ProfileOtpSendRequest request = new ProfileOtpSendRequest();
		request.setChannel("PHONE");
		request.setPhone("+919876543210");

		service.sendProfileOtp("me@kittyp.test", request);

		verify(phoneAvailabilityService).assertAvailable(eq("9876543210"), eq(current));
		verify(smsService).sendOtp("+919876543210", "123456", "me@kittyp.test");
	}
}
