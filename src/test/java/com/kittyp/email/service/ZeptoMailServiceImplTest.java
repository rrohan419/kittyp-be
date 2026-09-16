package com.kittyp.email.service;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.core.env.Environment;

import com.kittyp.common.constants.TemplateConstant;
import com.kittyp.common.util.VerificationCodeService;
import com.kittyp.email.ZeptoMergeFields;
import com.kittyp.email.dto.ZeptoMailDto;
import com.kittyp.email.emailsender.ZeptoMailSender;
import com.kittyp.email.model.ZeptoMailResponseModel;
import com.kittyp.order.dao.OrderDao;
import com.kittyp.user.dao.UserDao;
import com.kittyp.user.entity.User;

class ZeptoMailServiceImplTest {

	private ZeptoMailSender sender;
	private EmailAuditService auditService;
	private UserDao userDao;
	private Environment env;
	private ZeptoMailServiceImpl mailService;

	@BeforeEach
	void setUp() {
		sender = mock(ZeptoMailSender.class);
		auditService = mock(EmailAuditService.class);
		userDao = mock(UserDao.class);
		env = mock(Environment.class);
		when(env.getProperty(TemplateConstant.ZOHO_CLINIC_ADMIN_WELCOME_EMAIL_TEMPLATE_ID))
				.thenReturn("tmpl-clinic-admin-welcome");
		when(env.getProperty(TemplateConstant.ZOHO_EMAIL_CHANGE_OTP_TEMPLATE_ID)).thenReturn("tmpl-email-otp");
		when(env.getProperty(TemplateConstant.ZOHO_DOCTOR_PROFILE_VERIFIED_TEMPLATE_ID)).thenReturn("tmpl-doc-verified");
		mailService = new ZeptoMailServiceImpl(sender, auditService, userDao, new VerificationCodeService(),
				mock(OrderDao.class), env);

		ZeptoMailResponseModel.ZeptoMailData data = new ZeptoMailResponseModel.ZeptoMailData();
		data.setCode("OK");
		data.setMessage("sent");
		ZeptoMailResponseModel response = new ZeptoMailResponseModel();
		response.setRequestId("req-1");
		response.setData(java.util.List.of(data));
		when(sender.sendEmail(any())).thenReturn(response);
	}

	@Test
	void sendDoctorProfileVerified_emptyTemplateKey_doesNotThrow() {
		when(env.getProperty(TemplateConstant.ZOHO_DOCTOR_PROFILE_VERIFIED_TEMPLATE_ID)).thenReturn("");
		assertDoesNotThrow(() -> mailService.sendDoctorProfileVerified("doc@kittyp.test", "Dr", "https://kittyp.in"));
		verify(sender, never()).sendEmail(any());
	}

	@Test
	void sendEmailChangeOtp_usesSpecMergeKeys() {
		mailService.sendEmailChangeOtp("new@kittyp.test", "Doc", "KittyP", "654321");
		ArgumentCaptor<ZeptoMailDto> captor = ArgumentCaptor.forClass(ZeptoMailDto.class);
		verify(sender).sendEmail(captor.capture());
		assertEquals("Doc", captor.getValue().getMergeInfo().get(ZeptoMergeFields.CUSTOMER_NAME));
		assertEquals("654321", captor.getValue().getMergeInfo().get(ZeptoMergeFields.OTP));
	}

	@Test
	void sendWelcomeEmailforClinicAdmin_dispatchesAndAuditsOnce() {
		User user = new User();
		user.setFirstName("Admin");
		when(userDao.userByEmail("clinic@kittyp.test")).thenReturn(user);

		mailService.sendWelcomeEmailforClinicAdmin("clinic@kittyp.test");

		verify(sender, times(1)).sendEmail(any());
		verify(auditService, times(1)).saveEmailAudit(any());
	}
}
