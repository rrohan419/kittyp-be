package com.kittyp.email.service;

import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.env.Environment;

import com.kittyp.common.constants.TemplateConstant;
import com.kittyp.email.emailsender.ZeptoMailSender;

@ExtendWith(MockitoExtension.class)
class ZeptoMailServiceImplAppointmentEmailTest {

	@Mock
	private ZeptoMailSender zeptoMailSender;
	@Mock
	private Environment env;

	@InjectMocks
	private ZeptoMailServiceImpl zeptoMailService;

	@Test
	void appointmentEmail_skippedWhenTemplateMissing() {
		when(env.getProperty(TemplateConstant.ZOHO_APPOINTMENT_BOOKED_EMAIL_TEMPLATE_ID)).thenReturn(" ");

		zeptoMailService.sendAppointmentBookedEmail("pat@example.com", "Pat", "Branch", "Milo", "Now", "your doctor");

		verify(zeptoMailSender, never()).sendEmail(org.mockito.ArgumentMatchers.any());
	}
}
