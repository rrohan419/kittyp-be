package com.kittyp.email.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;

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
