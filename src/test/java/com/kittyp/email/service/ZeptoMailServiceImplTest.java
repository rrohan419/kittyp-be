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
