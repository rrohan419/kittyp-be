package com.kittyp.email.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.InputStream;
import java.util.List;
import java.util.Properties;

import org.junit.jupiter.api.Test;
import org.springframework.core.env.Environment;

import com.kittyp.common.constants.TemplateConstant;
import com.kittyp.common.util.VerificationCodeService;
import com.kittyp.email.dto.EmailAuditDto;
import com.kittyp.email.dto.ZeptoMailDto;
import com.kittyp.email.emailsender.ZeptoMailSender;
import com.kittyp.email.model.ZeptoMailResponseModel;
import com.kittyp.order.dao.OrderDao;
import com.kittyp.user.dao.UserDao;

import org.mockito.ArgumentCaptor;

class ZeptoMailServiceImplTest {

	@Test
	void clinicVerificationEmailAuditsIncludeProvider() {
		ZeptoMailSender sender = mock(ZeptoMailSender.class);
		EmailAuditService auditService = mock(EmailAuditService.class);
		Environment environment = mock(Environment.class);
		when(environment.getProperty(TemplateConstant.ZOHO_CLINIC_PROFILE_VERIFIED_TEMPLATE_ID))
				.thenReturn("clinic-verified-template");
		when(sender.sendEmail(any())).thenReturn(response());
		ZeptoMailServiceImpl service = new ZeptoMailServiceImpl(
				sender,
				auditService,
				mock(UserDao.class),
				mock(VerificationCodeService.class),
				mock(OrderDao.class),
				environment);

		service.sendClinicProfileVerified("clinic@example.com", "Clinic Admin", "Paws Clinic",
				"https://example.com/clinic");

		ArgumentCaptor<EmailAuditDto> auditCaptor = ArgumentCaptor.forClass(EmailAuditDto.class);
		verify(auditService).saveEmailAudit(auditCaptor.capture());
		assertEquals("ZeptoMail", auditCaptor.getValue().getProvider());
		assertEquals("request-123", auditCaptor.getValue().getRequestId());
	}

	@Test
	void emailChangeOtpUsesDedicatedTemplateAndMergeInfo() {
		ZeptoMailSender sender = mock(ZeptoMailSender.class);
		EmailAuditService auditService = mock(EmailAuditService.class);
		Environment environment = mock(Environment.class);
		when(environment.getProperty(TemplateConstant.ZEPTO_EMAIL_CHANGE_OTP_TEMPLATE_ID))
				.thenReturn("email-change-otp-template");
		when(sender.sendEmail(any())).thenReturn(response());
		ZeptoMailServiceImpl service = new ZeptoMailServiceImpl(
				sender,
				auditService,
				mock(UserDao.class),
				mock(VerificationCodeService.class),
				mock(OrderDao.class),
				environment);

		service.sendUpdateOtpEmail("new@example.com", "123456", "Rohan", "Clinic", "new@example.com");

		ArgumentCaptor<ZeptoMailDto> mailCaptor = ArgumentCaptor.forClass(ZeptoMailDto.class);
		verify(sender).sendEmail(mailCaptor.capture());
		ZeptoMailDto mail = mailCaptor.getValue();
		assertEquals("email-change-otp-template", mail.getTemplateKey());
		assertEquals("new@example.com", mail.getRecipientEmail());
		assertEquals("Rohan", mail.getRecipientName());
		assertEquals("Clinic", mail.getMergeInfo().get("account_type"));
		assertEquals("10", mail.getMergeInfo().get("expiry_minutes"));
		assertEquals("123456", mail.getMergeInfo().get("otp"));
		assertEquals("Rohan", mail.getMergeInfo().get("customer_name"));
		assertEquals("new@example.com", mail.getMergeInfo().get("new_email"));
	}

	@Test
	void clinicRejectionEmailIncludesExpectedMergeInfo() {
		ZeptoMailSender sender = mock(ZeptoMailSender.class);
		EmailAuditService auditService = mock(EmailAuditService.class);
		Environment environment = mock(Environment.class);
		when(environment.getProperty(TemplateConstant.ZOHO_CLINIC_PROFILE_REJECTED_TEMPLATE_ID))
				.thenReturn("clinic-rejected-template");
		when(sender.sendEmail(any())).thenReturn(response());
		ZeptoMailServiceImpl service = new ZeptoMailServiceImpl(
				sender,
				auditService,
				mock(UserDao.class),
				mock(VerificationCodeService.class),
				mock(OrderDao.class),
				environment);

		service.sendClinicProfileRejected(
				"owner@example.com", "Clinic Admin", "Paws Clinic", "Please upload a valid license.");

		ArgumentCaptor<ZeptoMailDto> mailCaptor = ArgumentCaptor.forClass(ZeptoMailDto.class);
		verify(sender).sendEmail(mailCaptor.capture());
		assertEquals("clinic-rejected-template", mailCaptor.getValue().getTemplateKey());
		assertEquals("Clinic Admin", mailCaptor.getValue().getMergeInfo().get("customer_name"));
		assertEquals("Paws Clinic", mailCaptor.getValue().getMergeInfo().get("clinic_name"));
		assertEquals("Please upload a valid license.",
				mailCaptor.getValue().getMergeInfo().get("rejection_reason"));
	}

	@Test
	void clinicPetParentInviteUsesProvidedTemplateAndMergeInfo() {
		ZeptoMailSender sender = mock(ZeptoMailSender.class);
		EmailAuditService auditService = mock(EmailAuditService.class);
		Environment environment = mock(Environment.class);
		when(environment.getProperty(TemplateConstant.ZEPTO_CLINIC_PET_PARENT_INVITE_EMAIL_TEMPLATE_ID))
				.thenReturn("pet-parent-invite-template");
		when(sender.sendEmail(any())).thenReturn(response());
		ZeptoMailServiceImpl service = new ZeptoMailServiceImpl(
				sender,
				auditService,
				mock(UserDao.class),
				mock(VerificationCodeService.class),
				mock(OrderDao.class),
				environment);

		service.sendClinicPetParentInviteEmail("owner@example.com", "Rohan", "Milo", "Happy Paws",
				"https://kittyp.in/signup?role=USER&email=owner%40example.com");

		ArgumentCaptor<ZeptoMailDto> mailCaptor = ArgumentCaptor.forClass(ZeptoMailDto.class);
		verify(sender).sendEmail(mailCaptor.capture());
		ZeptoMailDto mail = mailCaptor.getValue();
		assertEquals("pet-parent-invite-template", mail.getTemplateKey());
		assertEquals("owner@example.com", mail.getRecipientEmail());
		assertEquals("Rohan", mail.getRecipientName());
		assertEquals("Rohan", mail.getMergeInfo().get("customer_name"));
		assertEquals("Milo", mail.getMergeInfo().get("pet_name"));
		assertEquals("Happy Paws", mail.getMergeInfo().get("clinic_name"));
		assertEquals("https://kittyp.in/signup?role=USER&email=owner%40example.com",
				mail.getMergeInfo().get("invite_url"));
	}

	@Test
	void doctorRejectionEmailIncludesExpectedMergeInfo() {
		ZeptoMailSender sender = mock(ZeptoMailSender.class);
		EmailAuditService auditService = mock(EmailAuditService.class);
		Environment environment = mock(Environment.class);
		when(environment.getProperty(TemplateConstant.ZOHO_DOCTOR_PROFILE_REJECTED_TEMPLATE_ID))
				.thenReturn("doctor-rejected-template");
		when(sender.sendEmail(any())).thenReturn(response());
		ZeptoMailServiceImpl service = new ZeptoMailServiceImpl(
				sender,
				auditService,
				mock(UserDao.class),
				mock(VerificationCodeService.class),
				mock(OrderDao.class),
				environment);

		service.sendDoctorProfileRejected(
				"doctor@example.com", "Dr Casey", "Registration certificate is unreadable.");

		ArgumentCaptor<ZeptoMailDto> mailCaptor = ArgumentCaptor.forClass(ZeptoMailDto.class);
		verify(sender).sendEmail(mailCaptor.capture());
		assertEquals("doctor-rejected-template", mailCaptor.getValue().getTemplateKey());
		assertEquals("Dr Casey", mailCaptor.getValue().getMergeInfo().get("customer_name"));
		assertEquals("Registration certificate is unreadable.",
				mailCaptor.getValue().getMergeInfo().get("rejection_reason"));
	}

	@Test
	void staffInviteAcceptedUsesAcceptedTemplateAndMergeFields() {
		ZeptoMailSender sender = mock(ZeptoMailSender.class);
		Environment environment = mock(Environment.class);
		when(environment.getProperty(TemplateConstant.ZEPTO_CLINIC_STAFF_INVITE_ACCEPTED_EMAIL_TEMPLATE_ID))
				.thenReturn("staff-accepted-template");
		when(sender.sendEmail(any())).thenReturn(response());
		ZeptoMailServiceImpl service = mailService(sender, environment);

		service.sendClinicStaffInviteResponseEmail(
				"clinic@example.com", "Happy Paws", "Sam Staff", "staff@example.com", true);

		ArgumentCaptor<ZeptoMailDto> mailCaptor = ArgumentCaptor.forClass(ZeptoMailDto.class);
		verify(sender).sendEmail(mailCaptor.capture());
		ZeptoMailDto mail = mailCaptor.getValue();
		assertEquals("staff-accepted-template", mail.getTemplateKey());
		assertEquals("clinic@example.com", mail.getRecipientEmail());
		assertEquals("Happy Paws", mail.getRecipientName());
		assertEquals("Sam Staff", mail.getMergeInfo().get("staff_name"));
		assertEquals("staff@example.com", mail.getMergeInfo().get("staff_email"));
		assertEquals("Happy Paws", mail.getMergeInfo().get("clinic_name"));
		assertEquals("accepted", mail.getMergeInfo().get("status"));
	}

	@Test
	void staffInviteDeclinedUsesDeclinedTemplate() {
		ZeptoMailSender sender = mock(ZeptoMailSender.class);
		Environment environment = mock(Environment.class);
		when(environment.getProperty(TemplateConstant.ZEPTO_CLINIC_STAFF_INVITE_DECLINED_EMAIL_TEMPLATE_ID))
				.thenReturn("staff-declined-template");
		when(sender.sendEmail(any())).thenReturn(response());
		ZeptoMailServiceImpl service = mailService(sender, environment);

		service.sendClinicStaffInviteResponseEmail(
				"clinic@example.com", "Happy Paws", "Sam Staff", "staff@example.com", false);

		ArgumentCaptor<ZeptoMailDto> mailCaptor = ArgumentCaptor.forClass(ZeptoMailDto.class);
		verify(sender).sendEmail(mailCaptor.capture());
		assertEquals("staff-declined-template", mailCaptor.getValue().getTemplateKey());
		assertEquals("declined", mailCaptor.getValue().getMergeInfo().get("status"));
	}

	@Test
	void staffInviteResponseSkipsBlankRecipientAndMissingTemplate() {
		ZeptoMailSender sender = mock(ZeptoMailSender.class);
		Environment environment = mock(Environment.class);
		ZeptoMailServiceImpl service = mailService(sender, environment);

		service.sendClinicStaffInviteResponseEmail("  ", "Happy Paws", "Sam Staff", "staff@example.com", true);
		verify(sender, never()).sendEmail(any());

		when(environment.getProperty(TemplateConstant.ZEPTO_CLINIC_STAFF_INVITE_DECLINED_EMAIL_TEMPLATE_ID))
				.thenReturn("  ");
		service.sendClinicStaffInviteResponseEmail(
				"clinic@example.com", "Happy Paws", "Sam Staff", "staff@example.com", false);
		verify(sender, never()).sendEmail(any());
	}

	@Test
	void staffInviteResponseSwallowsSenderFailure() {
		ZeptoMailSender sender = mock(ZeptoMailSender.class);
		Environment environment = mock(Environment.class);
		when(environment.getProperty(TemplateConstant.ZEPTO_CLINIC_STAFF_INVITE_ACCEPTED_EMAIL_TEMPLATE_ID))
				.thenReturn("staff-accepted-template");
		doThrow(new RuntimeException("zepto down")).when(sender).sendEmail(any());
		ZeptoMailServiceImpl service = mailService(sender, environment);

		service.sendClinicStaffInviteResponseEmail(
				"clinic@example.com", "Happy Paws", "Sam Staff", "staff@example.com", true);

		verify(sender).sendEmail(any());
	}

	@Test
	void liveProfilesDefineDistinctStaffInviteResponseTemplates() throws Exception {
		Properties local = loadProperties("/application-local.properties");
		Properties dev = loadProperties("/application-dev.properties");
		String accepted = "2518b.1c420b141cfc0862.k1.b6937da0-c8ae-11f1-a992-d2cf08f4ca8c.1a140389a7a";
		String declined = "2518b.1c420b141cfc0862.k1.dcb8f690-c8ae-11f1-b523-62df313bf14d.1a140399479";

		assertEquals(accepted, local.getProperty(TemplateConstant.ZEPTO_CLINIC_STAFF_INVITE_ACCEPTED_EMAIL_TEMPLATE_ID));
		assertEquals(declined, local.getProperty(TemplateConstant.ZEPTO_CLINIC_STAFF_INVITE_DECLINED_EMAIL_TEMPLATE_ID));
		assertEquals(accepted, dev.getProperty(TemplateConstant.ZEPTO_CLINIC_STAFF_INVITE_ACCEPTED_EMAIL_TEMPLATE_ID));
		assertEquals(declined, dev.getProperty(TemplateConstant.ZEPTO_CLINIC_STAFF_INVITE_DECLINED_EMAIL_TEMPLATE_ID));
		assertNull(local.getProperty("zoho.clinic.staff.invite.response.email.template.id"));
	}

	private static ZeptoMailServiceImpl mailService(ZeptoMailSender sender, Environment environment) {
		return new ZeptoMailServiceImpl(
				sender,
				mock(EmailAuditService.class),
				mock(UserDao.class),
				mock(VerificationCodeService.class),
				mock(OrderDao.class),
				environment);
	}

	private static Properties loadProperties(String classpath) throws Exception {
		Properties properties = new Properties();
		try (InputStream in = ZeptoMailServiceImplTest.class.getResourceAsStream(classpath)) {
			if (in == null) {
				throw new IllegalStateException("Missing " + classpath);
			}
			properties.load(in);
		}
		return properties;
	}

	private static ZeptoMailResponseModel response() {
		ZeptoMailResponseModel.ZeptoMailData data = new ZeptoMailResponseModel.ZeptoMailData();
		data.setCode("OK");
		data.setMessage("email sent");
		ZeptoMailResponseModel response = new ZeptoMailResponseModel();
		response.setData(List.of(data));
		response.setMessage("success");
		response.setRequestId("request-123");
		return response;
	}
}
