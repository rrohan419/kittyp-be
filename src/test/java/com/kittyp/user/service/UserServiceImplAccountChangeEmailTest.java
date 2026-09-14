package com.kittyp.user.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

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
import com.kittyp.user.dto.UpdatePasswordDto;
import com.kittyp.user.dto.UserDetailDto;
import com.kittyp.user.entity.User;

class UserServiceImplAccountChangeEmailTest {

	private UserDao userDao;
	private VerificationCodeService verificationCodeService;
	private PasswordEncoder encoder;
	private ZeptoMailService zeptoMailService;
	private ClinicOwnerUserLinkService clinicOwnerUserLinkService;
	private UserServiceImpl userService;

	@BeforeEach
	void setUp() {
		userDao = mock(UserDao.class);
		verificationCodeService = new VerificationCodeService();
		encoder = mock(PasswordEncoder.class);
		zeptoMailService = mock(ZeptoMailService.class);
		clinicOwnerUserLinkService = mock(ClinicOwnerUserLinkService.class);
		when(encoder.encode(any())).thenReturn("encoded");
		when(userDao.saveUser(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

		userService = new UserServiceImpl(
				userDao,
				mock(RoleDao.class),
				verificationCodeService,
				encoder,
				zeptoMailService,
				mock(FcmPushNotificationService.class),
				mock(UserFcmTokenDao.class),
				mock(JwtUtils.class),
				mock(SmsService.class),
				mock(MasterTotpService.class),
				clinicOwnerUserLinkService);
	}

	@Test
	void updatePassword_sendsChangedEmailAfterSave() {
		User user = user("u-1", "doc@kittyp.test", "Doc");
		when(userDao.userByEmail("doc@kittyp.test")).thenReturn(user);
		String code = verificationCodeService.generateCode(user.getUuid());

		assertTrue(userService.updatePassword(passwordDto("doc@kittyp.test", code, "NewPass1!")));

		verify(userDao).saveUser(user);
		verify(zeptoMailService).sendPasswordChangedEmail("doc@kittyp.test", "Doc");
	}

	@Test
	void updatePassword_invalidCode_doesNotSendEmail() {
		User user = user("u-1", "doc@kittyp.test", "Doc");
		when(userDao.userByEmail("doc@kittyp.test")).thenReturn(user);
		verificationCodeService.generateCode(user.getUuid());

		assertThrows(CustomException.class,
				() -> userService.updatePassword(passwordDto("doc@kittyp.test", "000000", "NewPass1!")));

		verify(userDao, never()).saveUser(any());
		verify(zeptoMailService, never()).sendPasswordChangedEmail(any(), any());
	}

	@Test
	void updateUserDetail_phoneChange_sendsChangedEmailAfterSave() {
		User user = user("u-2", "doc@kittyp.test", "Doc");
		user.setPhoneCountryCode("+91");
		user.setPhoneNumber("9876543210");
		when(userDao.userByEmail("doc@kittyp.test")).thenReturn(user);
		verificationCodeService.markVerified(
				VerificationCodeService.profilePhoneVerifiedKey("u-2", "+919111111111"));

		UserDetailDto dto = new UserDetailDto();
		dto.setPhoneCountryCode("+91");
		dto.setPhoneNumber("9111111111");

		userService.updateUserDetail("doc@kittyp.test", dto);

		verify(zeptoMailService).sendPhoneChangedEmail("doc@kittyp.test", "Doc", "+919111111111");
	}

	@Test
	void updateUserDetail_samePhone_doesNotSendEmail() {
		User user = user("u-2", "doc@kittyp.test", "Doc");
		user.setPhoneCountryCode("+91");
		user.setPhoneNumber("9876543210");
		when(userDao.userByEmail("doc@kittyp.test")).thenReturn(user);

		UserDetailDto dto = new UserDetailDto();
		dto.setFirstName("Doc");
		dto.setPhoneCountryCode("+91");
		dto.setPhoneNumber("9876543210");

		userService.updateUserDetail("doc@kittyp.test", dto);

		verify(zeptoMailService, never()).sendPhoneChangedEmail(any(), any(), any());
	}

	@Test
	void updateUserDetail_unverifiedPhone_doesNotSendEmail() {
		User user = user("u-2", "doc@kittyp.test", "Doc");
		user.setPhoneCountryCode("+91");
		user.setPhoneNumber("9876543210");
		when(userDao.userByEmail("doc@kittyp.test")).thenReturn(user);

		UserDetailDto dto = new UserDetailDto();
		dto.setPhoneCountryCode("+91");
		dto.setPhoneNumber("9111111111");

		CustomException ex = assertThrows(CustomException.class,
				() -> userService.updateUserDetail("doc@kittyp.test", dto));
		assertEquals("Phone re-verification required before changing phone number", ex.getMessage());
		verify(zeptoMailService, never()).sendPhoneChangedEmail(any(), any(), any());
	}

	private static User user(String uuid, String email, String firstName) {
		User user = new User();
		user.setUuid(uuid);
		user.setEmail(email);
		user.setFirstName(firstName);
		user.setPassword("old");
		return user;
	}

	private static UpdatePasswordDto passwordDto(String email, String code, String password) {
		UpdatePasswordDto dto = new UpdatePasswordDto();
		ReflectionTestUtils.setField(dto, "email", email);
		ReflectionTestUtils.setField(dto, "code", code);
		ReflectionTestUtils.setField(dto, "password", password);
		return dto;
	}
}
