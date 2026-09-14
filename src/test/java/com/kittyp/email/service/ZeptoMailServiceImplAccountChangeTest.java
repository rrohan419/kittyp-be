package com.kittyp.email.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.core.env.Environment;

import com.kittyp.common.util.VerificationCodeService;
import com.kittyp.email.emailsender.ZeptoMailSender;
import com.kittyp.email.model.ZeptoMailResponseModel;
import com.kittyp.order.dao.OrderDao;
import com.kittyp.user.dao.UserDao;
import com.kittyp.user.entity.User;

class ZeptoMailServiceImplAccountChangeTest {

	private ZeptoMailSender sender;
	private EmailAuditService auditService;
	private UserDao userDao;
	private VerificationCodeService verificationCodeService;
	private ZeptoMailServiceImpl mailService;

	@BeforeEach
	void setUp() {
		sender = mock(ZeptoMailSender.class);
		auditService = mock(EmailAuditService.class);
		userDao = mock(UserDao.class);
		verificationCodeService = new VerificationCodeService();
		mailService = new ZeptoMailServiceImpl(
				sender,
				auditService,
				userDao,
				verificationCodeService,
				mock(OrderDao.class),
				mock(Environment.class));

		ZeptoMailResponseModel.ZeptoMailData data = new ZeptoMailResponseModel.ZeptoMailData();
		data.setCode("OK");
		data.setMessage("sent");
		ZeptoMailResponseModel response = new ZeptoMailResponseModel();
		response.setMessage("ok");
		response.setRequestId("req-1");
		response.setData(java.util.List.of(data));
		when(sender.sendHtmlEmail(any(), any(), any(), any())).thenReturn(response);
	}

	@Test
	void sendPasswordChangedEmail_sendsHtmlWithSubject() {
		mailService.sendPasswordChangedEmail("doc@kittyp.test", "Doc");

		ArgumentCaptor<String> subject = ArgumentCaptor.forClass(String.class);
		ArgumentCaptor<String> html = ArgumentCaptor.forClass(String.class);
		verify(sender).sendHtmlEmail(eq("doc@kittyp.test"), eq("Doc"), subject.capture(), html.capture());
		assertEquals("Your Kittyp password was changed", subject.getValue());
		assertTrue(html.getValue().contains("Your Kittyp password was changed"));
		verify(sender, never()).sendEmail(any());
	}

	@Test
	void sendPhoneChangedEmail_includesNewNumber() {
		mailService.sendPhoneChangedEmail("doc@kittyp.test", "Doc", "+919111111111");

		ArgumentCaptor<String> subject = ArgumentCaptor.forClass(String.class);
		ArgumentCaptor<String> html = ArgumentCaptor.forClass(String.class);
		verify(sender).sendHtmlEmail(eq("doc@kittyp.test"), eq("Doc"), subject.capture(), html.capture());
		assertEquals("Your Kittyp phone number was changed", subject.getValue());
		assertTrue(html.getValue().contains("+919111111111"));
		verify(sender, never()).sendEmail(any());
	}

	@Test
	void sendPasswordChangedEmail_blankRecipient_skipsSend() {
		mailService.sendPasswordChangedEmail("  ", "Doc");
		verify(sender, never()).sendHtmlEmail(any(), any(), any(), any());
		verify(sender, never()).sendEmail(any());
	}

	@Test
	void sendPasswordResetCode_sendsHtmlWithCode() {
		User user = new User();
		user.setUuid("u-reset");
		user.setEmail("doc@kittyp.test");
		user.setFirstName("Doc");
		when(userDao.userByEmail("doc@kittyp.test")).thenReturn(user);

		mailService.sendPasswordResetCode("doc@kittyp.test");

		ArgumentCaptor<String> subject = ArgumentCaptor.forClass(String.class);
		ArgumentCaptor<String> html = ArgumentCaptor.forClass(String.class);
		verify(sender).sendHtmlEmail(eq("doc@kittyp.test"), eq("Doc"), subject.capture(), html.capture());
		assertEquals("Your Kittyp password reset code", subject.getValue());
		assertTrue(html.getValue().matches("(?s).*\\b\\d{6}\\b.*"));
		verify(sender, never()).sendEmail(any());
	}
}
