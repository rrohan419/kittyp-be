package com.kittyp.email.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
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

import com.kittyp.common.constants.AppConstant;
import com.kittyp.common.constants.TemplateConstant;
import com.kittyp.common.util.VerificationCodeService;
import com.kittyp.email.dto.ZeptoMailDto;
import com.kittyp.email.emailsender.ZeptoMailSender;
import com.kittyp.email.model.ZeptoMailResponseModel;
import com.kittyp.order.dao.OrderDao;
import com.kittyp.user.dao.UserDao;
import com.kittyp.user.entity.User;

class ZeptoMailServiceImplAccountChangeTest {

	private static final String SIGNUP_OTP_TEMPLATE = "tmpl-signup-otp";
	private static final String PASSWORD_RESET_TEMPLATE = "tmpl-password-reset";
	private static final String CLINIC_ADMIN_WELCOME = "tmpl-clinic-admin-welcome";
	private static final String PASSWORD_CHANGED = "tmpl-password-changed";
	private static final String PHONE_CHANGED = "tmpl-phone-changed";
	private static final String EMAIL_CHANGE_OTP = "tmpl-email-change-otp";
	private static final String INVITE_REMINDER = "tmpl-invite-reminder";
	private static final String DOCTOR_VERIFIED = "tmpl-doctor-verified";
	private static final String CLINIC_VERIFIED = "tmpl-clinic-verified";
	private static final String INVOICE = "tmpl-invoice";
	private static final String STAFF_INVITE = "tmpl-staff-invite";
	private static final String PET_CONSENT = "tmpl-pet-consent";

	private ZeptoMailSender sender;
	private EmailAuditService auditService;
	private UserDao userDao;
	private VerificationCodeService verificationCodeService;
	private Environment env;
	private ZeptoMailServiceImpl mailService;

	@BeforeEach
	void setUp() {
		sender = mock(ZeptoMailSender.class);
		auditService = mock(EmailAuditService.class);
		userDao = mock(UserDao.class);
		env = mock(Environment.class);
		verificationCodeService = new VerificationCodeService();
		when(env.getProperty(TemplateConstant.ZEPTO_SIGNUP_OTP_EMAIL_TEMPLATE_ID)).thenReturn(SIGNUP_OTP_TEMPLATE);
		when(env.getProperty(TemplateConstant.ZEPTO_RESET_PASSWORD_CODE_EMAIL_TEMPLATE_ID))
				.thenReturn(PASSWORD_RESET_TEMPLATE);
		when(env.getProperty(TemplateConstant.ZOHO_CLINIC_ADMIN_WELCOME_EMAIL_TEMPLATE_ID))
				.thenReturn(CLINIC_ADMIN_WELCOME);
		when(env.getProperty(TemplateConstant.ZOHO_ACCOUNT_PASSWORD_CHANGED_TEMPLATE_ID)).thenReturn(PASSWORD_CHANGED);
		when(env.getProperty(TemplateConstant.ZOHO_ACCOUNT_PHONE_CHANGED_TEMPLATE_ID)).thenReturn(PHONE_CHANGED);
		when(env.getProperty(TemplateConstant.ZOHO_EMAIL_CHANGE_OTP_TEMPLATE_ID)).thenReturn(EMAIL_CHANGE_OTP);
		when(env.getProperty(TemplateConstant.ZEPTO_CLINIC_DOCTOR_INVITE_REMINDER_EMAIL_TEMPLATE_ID))
				.thenReturn(INVITE_REMINDER);
		when(env.getProperty(TemplateConstant.ZOHO_DOCTOR_PROFILE_VERIFIED_TEMPLATE_ID)).thenReturn(DOCTOR_VERIFIED);
		when(env.getProperty(TemplateConstant.ZOHO_CLINIC_PROFILE_VERIFIED_TEMPLATE_ID)).thenReturn(CLINIC_VERIFIED);
		when(env.getProperty(TemplateConstant.ZOHO_TREATMENT_INVOICE_EMAIL_TEMPLATE_ID)).thenReturn(INVOICE);
		when(env.getProperty(TemplateConstant.ZEPTO_CLINIC_STAFF_INVITE_EMAIL_TEMPLATE_ID)).thenReturn(STAFF_INVITE);
		when(env.getProperty(TemplateConstant.ZEPTO_CLINIC_PET_CONSENT_OTP_EMAIL_TEMPLATE_ID)).thenReturn(PET_CONSENT);
		mailService = new ZeptoMailServiceImpl(
				sender,
				auditService,
				userDao,
				verificationCodeService,
				mock(OrderDao.class),
				env);

		ZeptoMailResponseModel.ZeptoMailData data = new ZeptoMailResponseModel.ZeptoMailData();
		data.setCode("OK");
		data.setMessage("sent");
		ZeptoMailResponseModel response = new ZeptoMailResponseModel();
		response.setMessage("ok");
		response.setRequestId("req-1");
		response.setData(java.util.List.of(data));
		when(sender.sendEmail(any())).thenReturn(response);
	}

	@Test
	void sendWelcomeEmailforClinicAdmin_sendsOnceWithAudit() {
		User user = new User();
		user.setFirstName("Admin");
		when(userDao.userByEmail("clinic@kittyp.test")).thenReturn(user);

		mailService.sendWelcomeEmailforClinicAdmin("clinic@kittyp.test");

		ZeptoMailDto mail = captureMail();
		assertEquals("clinic@kittyp.test", mail.getRecipientEmail());
		assertEquals(CLINIC_ADMIN_WELCOME, mail.getTemplateKey());
		assertEquals("Admin", mail.getMergeInfo().get("Customer_Name"));
		assertNull(mail.getMergeInfo().get("customer_name"));
		assertEquals(AppConstant.KITTYP_EMAIL_TEMPLATE_LOGO, mail.getMergeInfo().get("logo_url"));
		assertEquals(java.time.LocalDate.now().getYear(), mail.getMergeInfo().get("current_year"));
		verify(sender, times(1)).sendEmail(any());
		verify(auditService, times(1)).saveEmailAudit(any());
	}

	@Test
	void sendWelcomeEmailforClinicAdmin_blankTemplate_skipsSend() {
		when(env.getProperty(TemplateConstant.ZOHO_CLINIC_ADMIN_WELCOME_EMAIL_TEMPLATE_ID)).thenReturn("  ");
		User user = new User();
		user.setFirstName("Admin");
		when(userDao.userByEmail("clinic@kittyp.test")).thenReturn(user);

		mailService.sendWelcomeEmailforClinicAdmin("clinic@kittyp.test");

		verify(sender, never()).sendEmail(any());
		verify(auditService, never()).saveEmailAudit(any());
	}

	@Test
	void sendPasswordChangedNotification_usesDedicatedTemplate() {
		mailService.sendPasswordChangedNotification("doc@kittyp.test", "Doc", "KittyP Clinic", "15 Sep 2026, 03:00 pm");

		ZeptoMailDto mail = captureMail();
		assertEquals("doc@kittyp.test", mail.getRecipientEmail());
		assertEquals("Doc", mail.getRecipientName());
		assertEquals(PASSWORD_CHANGED, mail.getTemplateKey());
		assertEquals("Doc", mail.getMergeInfo().get("customer_name"));
		assertEquals("KittyP Clinic", mail.getMergeInfo().get("clinic_name"));
		assertEquals("15 Sep 2026, 03:00 pm", mail.getMergeInfo().get("change_time"));
		assertEquals("https://kittyp.in", mail.getMergeInfo().get("support_url"));
		assertEquals(java.time.LocalDate.now().getYear(), mail.getMergeInfo().get("current_year"));
		assertEquals(AppConstant.KITTYP_EMAIL_TEMPLATE_LOGO, mail.getMergeInfo().get("logo_url"));
		assertNull(mail.getSubject());
		assertNull(mail.getHtmlBody());
	}

	@Test
	void sendPhoneChangedNotification_includesNewNumber() {
		mailService.sendPhoneChangedNotification(
				"doc@kittyp.test", "Doc", "KittyP", "+919111111111", "https://kittyp.in/login");

		ZeptoMailDto mail = captureMail();
		assertEquals(PHONE_CHANGED, mail.getTemplateKey());
		assertEquals("+919111111111", mail.getMergeInfo().get("new_phone"));
		assertEquals("https://kittyp.in/login", mail.getMergeInfo().get("login_url"));
		assertNull(mail.getMergeInfo().get("change_time"));
		assertNull(mail.getMergeInfo().get("support_url"));
		assertEquals(AppConstant.KITTYP_EMAIL_TEMPLATE_LOGO, mail.getMergeInfo().get("logo_url"));
	}

	@Test
	void sendPasswordChangedNotification_blankRecipient_skipsSend() {
		mailService.sendPasswordChangedNotification("  ", "Doc", "KittyP", "now");
		verify(sender, never()).sendEmail(any());
	}

	@Test
	void sendPasswordChangedNotification_blankTemplate_skipsSend() {
		when(env.getProperty(TemplateConstant.ZOHO_ACCOUNT_PASSWORD_CHANGED_TEMPLATE_ID)).thenReturn("  ");
		mailService.sendPasswordChangedNotification("doc@kittyp.test", "Doc", "KittyP", "now");
		verify(sender, never()).sendEmail(any());
	}

	@Test
	void sendEmailChangeOtp_usesDedicatedTemplate() {
		mailService.sendEmailChangeOtp("new@kittyp.test", "Doc", "KittyP", "654321");

		ZeptoMailDto mail = captureMail();
		assertEquals(EMAIL_CHANGE_OTP, mail.getTemplateKey());
		assertEquals("654321", mail.getMergeInfo().get("otp"));
		assertEquals("Doc", mail.getMergeInfo().get("customer_name"));
		assertEquals(java.time.LocalDate.now().getYear(), mail.getMergeInfo().get("current_year"));
		assertEquals(AppConstant.KITTYP_EMAIL_TEMPLATE_LOGO, mail.getMergeInfo().get("logo_url"));
	}

	@Test
	void sendDoctorInviteReminder_usesReminderTemplateNotInvite() {
		mailService.sendClinicDoctorInviteReminderEmail(
				"doc@kittyp.test", "Dr Swapnil", "KittyP Clinic", "https://kittyp.in/accept");

		ZeptoMailDto mail = captureMail();
		assertEquals(INVITE_REMINDER, mail.getTemplateKey());
		assertEquals("Dr Swapnil", mail.getMergeInfo().get("doctor_name"));
		assertEquals("https://kittyp.in/accept", mail.getMergeInfo().get("accept_url"));
		assertEquals(java.time.LocalDate.now().getYear(), mail.getMergeInfo().get("current_year"));
		assertEquals(AppConstant.KITTYP_EMAIL_TEMPLATE_LOGO, mail.getMergeInfo().get("logo_url"));
	}

	@Test
	void sendDoctorProfileVerified_usesDedicatedTemplate() {
		mailService.sendDoctorProfileVerified("doc@kittyp.test", "Dr Swapnil", "https://kittyp.in/doctor");

		ZeptoMailDto mail = captureMail();
		assertEquals(DOCTOR_VERIFIED, mail.getTemplateKey());
		assertEquals("Dr Swapnil", mail.getMergeInfo().get("customer_name"));
		assertEquals("https://kittyp.in/doctor", mail.getMergeInfo().get("doctor_url"));
		assertEquals(AppConstant.KITTYP_EMAIL_TEMPLATE_LOGO, mail.getMergeInfo().get("logo_url"));
		assertNull(mail.getMergeInfo().get("doctor_name"));
		assertNull(mail.getMergeInfo().get("dashboard_url"));
	}

	@Test
	void sendPasswordResetCode_usesPasswordResetTemplateWithCode() {
		User user = new User();
		user.setUuid("u-reset");
		user.setEmail("doc@kittyp.test");
		user.setFirstName("Doc");
		when(userDao.userByEmail("doc@kittyp.test")).thenReturn(user);

		mailService.sendPasswordResetCode("doc@kittyp.test");

		ZeptoMailDto mail = captureMail();
		assertEquals("doc@kittyp.test", mail.getRecipientEmail());
		assertEquals("Doc", mail.getRecipientName());
		assertEquals(PASSWORD_RESET_TEMPLATE, mail.getTemplateKey());
		assertEquals("Doc", mail.getMergeInfo().get("customer_name"));
		assertEquals(AppConstant.KITTYP_EMAIL_TEMPLATE_LOGO, mail.getMergeInfo().get("logo_url"));
		assertEquals(java.time.LocalDate.now().getYear(), mail.getMergeInfo().get("current_year"));
		String code = (String) mail.getMergeInfo().get("reset_code");
		assertEquals(6, code.length());
		assertNull(mail.getMergeInfo().get("RESET_CODE"));
	}

	@Test
	void sendSignupOtpEmail_usesSignupOtpTemplateWithCode() {
		mailService.sendSignupOtpEmail("doc@kittyp.test", "654321", "DOCTOR", null);

		ZeptoMailDto mail = captureMail();
		assertEquals("doc@kittyp.test", mail.getRecipientEmail());
		assertEquals("Doctor Applicant", mail.getRecipientName());
		assertEquals(SIGNUP_OTP_TEMPLATE, mail.getTemplateKey());
		assertEquals("654321", mail.getMergeInfo().get("OTP"));
		assertNull(mail.getMergeInfo().get("otp"));
		assertEquals("Doctor Applicant", mail.getMergeInfo().get("Customer_Name"));
		assertNull(mail.getMergeInfo().get("customer_name"));
		assertEquals(AppConstant.KITTYP_EMAIL_TEMPLATE_LOGO, mail.getMergeInfo().get("logo_url"));
		assertEquals(java.time.LocalDate.now().getYear(), mail.getMergeInfo().get("currrent_year"));
		assertNull(mail.getMergeInfo().get("current_year"));
	}

	@Test
	void sendClinicStaffInviteEmail_sendsBothClinicNameTags() {
		mailService.sendClinicStaffInviteEmail("staff@kittyp.test", "Priya", "KittyP Clinic", "https://kittyp.in/accept");

		ZeptoMailDto mail = captureMail();
		assertEquals(STAFF_INVITE, mail.getTemplateKey());
		assertEquals("KittyP Clinic", mail.getMergeInfo().get("clinic_name"));
		assertEquals("KittyP Clinic", mail.getMergeInfo().get("Clinic_Name"));
		assertEquals("Priya", mail.getMergeInfo().get("staff_name"));
		assertEquals("https://kittyp.in/accept", mail.getMergeInfo().get("acceptUrl"));
		assertEquals(AppConstant.KITTYP_EMAIL_TEMPLATE_LOGO, mail.getMergeInfo().get("logo_url"));
	}

	@Test
	void sendClinicPetConsentOtpEmail_matchesAgentTags() {
		mailService.sendClinicPetConsentOtpEmail("owner@kittyp.test", "Asha", "KittyP Clinic", "Milo", "654321");

		ZeptoMailDto mail = captureMail();
		assertEquals(PET_CONSENT, mail.getTemplateKey());
		assertEquals("Asha", mail.getMergeInfo().get("customer_name"));
		assertEquals("654321", mail.getMergeInfo().get("otp"));
		assertEquals("KittyP Clinic", mail.getMergeInfo().get("clinic_name"));
		assertEquals("Milo", mail.getMergeInfo().get("pet_name"));
		assertEquals(AppConstant.KITTYP_EMAIL_TEMPLATE_LOGO, mail.getMergeInfo().get("logo_url"));
		assertEquals(java.time.LocalDate.now().getYear(), mail.getMergeInfo().get("current_year"));
	}

	@Test
	void sendClinicProfileVerified_usesDedicatedTemplate() {
		mailService.sendClinicProfileVerified("admin@kittyp.test", "Asha", "KittyP Clinic", "https://kittyp.in/clinic");

		ZeptoMailDto mail = captureMail();
		assertEquals(CLINIC_VERIFIED, mail.getTemplateKey());
		assertEquals("Asha", mail.getMergeInfo().get("customer_name"));
		assertEquals("KittyP Clinic", mail.getMergeInfo().get("clinic_name"));
		assertEquals("https://kittyp.in/clinic", mail.getMergeInfo().get("clinic_url"));
		assertEquals(AppConstant.KITTYP_EMAIL_TEMPLATE_LOGO, mail.getMergeInfo().get("logo_url"));
	}

	@Test
	void sendInvoiceEmail_blankRecipient_skipsSend() {
		mailService.sendInvoiceEmail("  ", "Asha", "Clinic", "Milo", "INV-1", "100.00",
				"https://kittyp-invoices.s3.ap-south-1.amazonaws.com/treatment-invoices/Invoice_INV-1.pdf",
				new byte[] { 1, 2, 3 }, "invoice.pdf");
		verify(sender, never()).sendEmail(any());
	}

	@Test
	void sendInvoiceEmail_attachesPdfAndExactMergeTags() {
		mailService.sendInvoiceEmail("owner@kittyp.test", "Asha", "KittyP Clinic", "Milo", "INV-2026-000027", "4999.69",
				"https://kittyp-invoices.s3.ap-south-1.amazonaws.com/treatment-invoices/Invoice_INV-2026-000027.pdf",
				new byte[] { 1, 2, 3 }, "invoice.pdf");

		ZeptoMailDto mail = captureMail();
		assertEquals(INVOICE, mail.getTemplateKey());
		assertEquals("Asha", mail.getMergeInfo().get("customer_name"));
		assertEquals("KittyP Clinic", mail.getMergeInfo().get("clinic_name"));
		assertEquals("Milo", mail.getMergeInfo().get("pet_name"));
		assertEquals("INV-2026-000027", mail.getMergeInfo().get("invoice_number"));
		assertEquals("4999.69", mail.getMergeInfo().get("amount"));
		assertEquals("https://kittyp-invoices.s3.ap-south-1.amazonaws.com/treatment-invoices/Invoice_INV-2026-000027.pdf",
				mail.getMergeInfo().get("invoice_url"));
		assertEquals(AppConstant.KITTYP_EMAIL_TEMPLATE_LOGO, mail.getMergeInfo().get("logo_url"));
		assertEquals(1, mail.getAttachments().size());
		assertEquals("application/pdf", mail.getAttachments().get(0).getMimeType());
		assertEquals("invoice.pdf", mail.getAttachments().get(0).getName());
	}

	private ZeptoMailDto captureMail() {
		ArgumentCaptor<ZeptoMailDto> captor = ArgumentCaptor.forClass(ZeptoMailDto.class);
		verify(sender).sendEmail(captor.capture());
		return captor.getValue();
	}
}
