package com.kittyp.email.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.clearInvocations;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.core.env.Environment;

import com.kittyp.common.constants.TemplateConstant;
import com.kittyp.common.util.VerificationCodeService;
import com.kittyp.email.dto.ZeptoMailDto;
import com.kittyp.email.emailsender.ZeptoMailSender;
import com.kittyp.email.model.ZeptoMailResponseModel;
import com.kittyp.order.dao.OrderDao;
import com.kittyp.order.entity.AddressInfo;
import com.kittyp.order.entity.Order;
import com.kittyp.order.entity.Taxes;
import com.kittyp.user.dao.UserDao;
import com.kittyp.user.entity.User;

class ZeptoTemplateKeyTest {

	private ZeptoMailSender sender;
	private EmailAuditService auditService;
	private UserDao userDao;
	private OrderDao orderDao;
	private Environment env;
	private ZeptoMailServiceImpl mailService;

	@BeforeEach
	void setUp() {
		sender = mock(ZeptoMailSender.class);
		auditService = mock(EmailAuditService.class);
		userDao = mock(UserDao.class);
		orderDao = mock(OrderDao.class);
		env = mock(Environment.class);
		when(env.getProperty(any())).thenAnswer(inv -> "key:" + inv.getArgument(0));
		mailService = new ZeptoMailServiceImpl(sender, auditService, userDao, new VerificationCodeService(), orderDao,
				env);
		ZeptoMailResponseModel.ZeptoMailData data = new ZeptoMailResponseModel.ZeptoMailData();
		data.setCode("OK");
		data.setMessage("sent");
		ZeptoMailResponseModel response = new ZeptoMailResponseModel();
		response.setRequestId("req-1");
		response.setData(List.of(data));
		when(sender.sendEmail(any())).thenReturn(response);
		when(userDao.userByEmail(any())).thenReturn(user());
	}

	@Test
	void eachConfiguredTemplateUsesItsOwnKeyAndMergeField() {
		mailService.sendWelcomeEmailforParent("Ada", "ada@kittyp.test");
		assertSent(TemplateConstant.ZOHO_PARENT_WELCOME_EMAIL_TEMPLATE_ID, "Customer_Name", "Ada");

		clearInvocations(sender);
		mailService.sendWelcomeEmailforDoctor("ada@kittyp.test");
		assertSent(TemplateConstant.ZOHO_DOCTOR_WELCOME_EMAIL_TEMPLATE_ID, "Customer_Name", "Ada");

		clearInvocations(sender);
		mailService.sendWelcomeEmailforClinicAdmin("ada@kittyp.test");
		assertSent(TemplateConstant.ZOHO_CLINIC_ADMIN_WELCOME_EMAIL_TEMPLATE_ID, "Customer_Name", "Ada");

		clearInvocations(sender);
		mailService.sendSignupOtpEmail("ada@kittyp.test", "123456", "email", null);
		assertSent(TemplateConstant.ZEPTO_SIGNUP_OTP_EMAIL_TEMPLATE_ID, "OTP", "123456");

		clearInvocations(sender);
		mailService.sendClinicPetConsentOtpEmail("ada@kittyp.test", "Ada", "Clinic", "Miso", "654321");
		assertSent(TemplateConstant.ZEPTO_CLINIC_PET_CONSENT_OTP_EMAIL_TEMPLATE_ID, "pet_name", "Miso");

		clearInvocations(sender);
		mailService.sendClinicDoctorInviteEmail("ada@kittyp.test", "Ravi", "Clinic", "https://kittyp.in/accept");
		assertSent(TemplateConstant.ZEPTO_CLINIC_DOCTOR_INVITE_EMAIL_TEMPLATE_ID, "acceptUrl",
				"https://kittyp.in/accept");

		clearInvocations(sender);
		mailService.sendClinicStaffInviteEmail("ada@kittyp.test", "Sam", "Clinic", "https://kittyp.in/staff");
		assertSent(TemplateConstant.ZEPTO_CLINIC_STAFF_INVITE_EMAIL_TEMPLATE_ID, "staff_name", "Sam");

		clearInvocations(sender);
		mailService.sendClinicDoctorInviteReminderEmail("ada@kittyp.test", "Ravi", "Clinic", "https://kittyp.in/accept");
		assertSent(TemplateConstant.ZEPTO_CLINIC_DOCTOR_INVITE_REMINDER_EMAIL_TEMPLATE_ID, "acceptUrl",
				"https://kittyp.in/accept");

		clearInvocations(sender);
		mailService.sendClinicDoctorInviteResponseEmail("ada@kittyp.test", "Clinic", "Ravi", "ravi@kittyp.test", true);
		assertSent(TemplateConstant.ZEPTO_CLINIC_DOCTOR_INVITE_RESPONSE_EMAIL_TEMPLATE_ID, "status", "accepted");

		clearInvocations(sender);
		when(orderDao.orderByOrderNumber("ORD-1")).thenReturn(order());
		mailService.sendOrderConfirmationEmail("ada@kittyp.test", "ORD-1");
		assertSent(TemplateConstant.ZEPTO_ORDER_CONFIRMATION_EMAIL_TEMPLATE_ID, "order_number", "ORD-1");

		clearInvocations(sender);
		mailService.sendPasswordResetCode("ada@kittyp.test");
		assertSent(TemplateConstant.ZEPTO_RESET_PASSWORD_CODE_EMAIL_TEMPLATE_ID, "customer_name", "Ada");

		clearInvocations(sender);
		mailService.sendClinicClientAttachOtpEmail("ada@kittyp.test", "Ada", "Clinic", "111222");
		assertSent(TemplateConstant.ZEPTO_CLINIC_CLIENT_CONSENT_EMAIL_TEMPLATE_ID, "otp", "111222");

		clearInvocations(sender);
		mailService.sendDoctorProfileVerified("ada@kittyp.test", "Ravi", "https://kittyp.in/doctor");
		assertSent(TemplateConstant.ZOHO_DOCTOR_PROFILE_VERIFIED_TEMPLATE_ID, "doctor_url", "https://kittyp.in/doctor");

		clearInvocations(sender);
		mailService.sendClinicProfileVerified("ada@kittyp.test", "Ada", "Clinic", "https://kittyp.in/clinic");
		assertSent(TemplateConstant.ZOHO_CLINIC_PROFILE_VERIFIED_TEMPLATE_ID, "clinic_url", "https://kittyp.in/clinic");

		clearInvocations(sender);
		mailService.sendInvoiceEmail("ada@kittyp.test", "Ada", "Clinic", "Miso", "INV-1", "500.00",
				"https://kittyp.in/inv", null, null);
		assertSent(TemplateConstant.ZOHO_TREATMENT_INVOICE_EMAIL_TEMPLATE_ID, "invoice_number", "INV-1");

		clearInvocations(sender);
		mailService.sendPasswordChangedNotification("ada@kittyp.test", "Ada", "Clinic", "1 Oct 2026");
		assertSent(TemplateConstant.ZOHO_ACCOUNT_PASSWORD_CHANGED_TEMPLATE_ID, "changed_at", "1 Oct 2026");

		clearInvocations(sender);
		mailService.sendPhoneChangedNotification("ada@kittyp.test", "Ada", "Clinic", "9999999999",
				"https://kittyp.in/login");
		assertSent(TemplateConstant.ZOHO_ACCOUNT_PHONE_CHANGED_TEMPLATE_ID, "phone", "9999999999");

		clearInvocations(sender);
		mailService.sendEmailChangeOtp("ada@kittyp.test", "Ada", "Clinic", "999000");
		assertSent(TemplateConstant.ZOHO_EMAIL_CHANGE_OTP_TEMPLATE_ID, "OTP", "999000");

		clearInvocations(sender);
		mailService.sendClinicParentCrmWelcomeEmail("Ada", "ada@kittyp.test", "Clinic");
		assertSent(TemplateConstant.ZEPTO_CLINIC_PARENT_CRM_WELCOME_EMAIL_TEMPLATE_ID, "clinic_name", "Clinic");

		clearInvocations(sender);
		mailService.sendWelcomeEmailforClinicStaff("Ada", "ada@kittyp.test", "Clinic");
		assertSent(TemplateConstant.ZEPTO_CLINIC_STAFF_WELCOME_EMAIL_TEMPLATE_ID, "clinic_name", "Clinic");
	}

	@Test
	void appointmentMailsCarryTheirActionFields() {
		mailService.sendAppointmentConfirmationEmail("ada@kittyp.test", "Ada", "Clinic", "Miso", "1 Oct 2026, 11:00 AM",
				"Ravi", "book-1", "12 Park Road, Pune", "https://maps.example/1",
				"https://kittyp.in/app/appointments/book-1/reschedule",
				"https://kittyp.in/app/appointments/book-1/cancel");
		ZeptoMailDto confirm = sent();
		assertEquals("key:" + TemplateConstant.ZEPTO_APPOINTMENT_CONFIRMATION_EMAIL_TEMPLATE_ID, confirm.getTemplateKey());
		assertEquals("https://kittyp.in/app/appointments/book-1/reschedule", confirm.getMergeInfo().get("reschedule_url"));
		assertEquals("https://kittyp.in/app/appointments/book-1/cancel", confirm.getMergeInfo().get("cancel_url"));

		clearInvocations(sender);
		assertTrue(mailService.sendAppointmentReminderEmail("ada@kittyp.test", "Ada", "Clinic", "Miso",
				"1 Oct 2026, 11:00 AM", "Ravi", "book-1", "9999999999", "12 Park Road, Pune",
				"https://maps.example/1"));
		ZeptoMailDto reminder = sent();
		assertEquals("key:" + TemplateConstant.ZEPTO_APPOINTMENT_REMINDER_EMAIL_TEMPLATE_ID, reminder.getTemplateKey());
		assertEquals("9999999999", reminder.getMergeInfo().get("clinic_phone"));
		assertEquals("https://maps.example/1", reminder.getMergeInfo().get("maps_url"));

		clearInvocations(sender);
		mailService.sendAppointmentRescheduledEmail("ada@kittyp.test", "Ada", "Clinic", "Miso", "2 Oct 2026, 11:00 AM",
				"Ravi", "book-1", "12 Park Road, Pune", "https://maps.example/1", "1 Oct 2026, 11:00 AM", "9999999999",
				"https://kittyp.in/app/appointments/book-1/reschedule");
		ZeptoMailDto moved = sent();
		assertEquals("key:" + TemplateConstant.ZEPTO_APPOINTMENT_RESCHEDULED_EMAIL_TEMPLATE_ID, moved.getTemplateKey());
		assertEquals("1 Oct 2026, 11:00 AM", moved.getMergeInfo().get("previous_appointment_when"));
		assertEquals("https://kittyp.in/app/appointments/book-1/reschedule", moved.getMergeInfo().get("manage_url"));

		clearInvocations(sender);
		mailService.sendAppointmentCancelledEmail("ada@kittyp.test", "Ada", "Clinic", "Miso", "1 Oct 2026, 11:00 AM",
				"Ravi", "book-1", "9999999999", "12 Park Road, Pune", "https://kittyp.in/app/book");
		ZeptoMailDto cancelled = sent();
		assertEquals("key:" + TemplateConstant.ZEPTO_APPOINTMENT_CANCELLED_EMAIL_TEMPLATE_ID, cancelled.getTemplateKey());
		assertEquals("https://kittyp.in/app/book", cancelled.getMergeInfo().get("book_url"));
		assertFalse(cancelled.getMergeInfo().containsKey("reschedule_url"));
		assertFalse(cancelled.getMergeInfo().containsKey("cancel_url"));

		clearInvocations(sender);
		mailService.sendWalkInCheckedInEmail("ada@kittyp.test", "Ada", "Clinic", "Miso", "Ravi", "visit-1",
				"9999999999", "12 Park Road, Pune", "https://maps.example/1");
		ZeptoMailDto walkIn = sent();
		assertEquals("key:" + TemplateConstant.ZEPTO_WALKIN_CHECKED_IN_EMAIL_TEMPLATE_ID, walkIn.getTemplateKey());
		assertEquals("9999999999", walkIn.getMergeInfo().get("clinic_phone"));
		assertFalse(walkIn.getMergeInfo().containsKey("reschedule_url"));

		clearInvocations(sender);
		mailService.sendAppointmentDoctorChangedEmail("ada@kittyp.test", "Ada", "Clinic", "Miso",
				"1 Oct 2026, 11:00 AM", "Ravi", "book-1", "doc-2", "12 Park Road, Pune", "https://maps.example/1",
				"9999999999", "https://kittyp.in/app/appointments/book-1/reschedule");
		ZeptoMailDto doctorChanged = sent();
		assertEquals("key:" + TemplateConstant.ZEPTO_APPOINTMENT_DOCTOR_CHANGED_EMAIL_TEMPLATE_ID,
				doctorChanged.getTemplateKey());
		assertEquals("https://kittyp.in/app/appointments/book-1/reschedule",
				doctorChanged.getMergeInfo().get("manage_url"));

		clearInvocations(sender);
		mailService.sendClinicClosureEmail("ada@kittyp.test", "Ada", "Clinic", "Miso", "1 Oct 2026, 11:00 AM", "Ravi",
				"book-1", "9999999999");
		ZeptoMailDto closure = sent();
		assertEquals("key:" + TemplateConstant.ZEPTO_CLINIC_CLOSURE_EMAIL_TEMPLATE_ID, closure.getTemplateKey());
		assertEquals("9999999999", closure.getMergeInfo().get("clinic_phone"));
		assertEquals("1 Oct 2026, 11:00 AM", closure.getMergeInfo().get("appointment_when"));

		clearInvocations(sender);
		mailService.sendInviteRevokedEmail("ada@kittyp.test", "Ada", "Clinic", "doctor", "invite-1");
		ZeptoMailDto revoked = sent();
		assertEquals("key:" + TemplateConstant.ZEPTO_INVITE_REVOKED_EMAIL_TEMPLATE_ID, revoked.getTemplateKey());
		assertEquals("doctor", revoked.getMergeInfo().get("invite_role"));
		assertEquals("Clinic", revoked.getMergeInfo().get("clinic_name"));

		clearInvocations(sender);
		mailService.sendClinicLocationChangedEmail("ada@kittyp.test", "Ada", "Clinic", "1 Oct 2026",
				"12 Park Road, Pune", "40 Lake Road, Pune", "https://maps.example/old", "https://maps.example/new",
				"clinic-1", "2026-10-01");
		ZeptoMailDto movedClinic = sent();
		assertEquals("key:" + TemplateConstant.ZEPTO_CLINIC_LOCATION_CHANGED_EMAIL_TEMPLATE_ID,
				movedClinic.getTemplateKey());
		assertEquals("12 Park Road, Pune", movedClinic.getMergeInfo().get("old_location"));
		assertEquals("https://maps.example/new", movedClinic.getMergeInfo().get("new_maps_url"));
		assertEquals("1 Oct 2026", movedClinic.getMergeInfo().get("move_date"));
	}

	@Test
	void blankClinicLocationTemplateDoesNotSend() {
		when(env.getProperty(TemplateConstant.ZEPTO_CLINIC_LOCATION_CHANGED_EMAIL_TEMPLATE_ID)).thenReturn("");
		mailService.sendClinicLocationChangedEmail("ada@kittyp.test", "Ada", "Clinic", "1 Oct 2026", "Old", "New",
				"https://maps.example/old", "https://maps.example/new", "clinic-1", "2026-10-01");
		verify(sender, never()).sendEmail(any());
	}

	@Test
	void blankInviteRevokedTemplateDoesNotSend() {
		when(env.getProperty(TemplateConstant.ZEPTO_INVITE_REVOKED_EMAIL_TEMPLATE_ID)).thenReturn("");
		mailService.sendInviteRevokedEmail("ada@kittyp.test", "Ada", "Clinic", "staff", "invite-1");
		verify(sender, never()).sendEmail(any());
	}

	@Test
	void blankAppointmentTemplateDoesNotSend() {
		when(env.getProperty(TemplateConstant.ZEPTO_APPOINTMENT_REMINDER_EMAIL_TEMPLATE_ID)).thenReturn("");
		assertFalse(mailService.sendAppointmentReminderEmail("ada@kittyp.test", "Ada", "Clinic", "Miso",
				"1 Oct 2026, 11:00 AM", "Ravi", "book-1", "999", "Pune", ""));
		verify(sender, never()).sendEmail(any());
	}

	@Test
	void duplicateAppointmentReminderIsTreatedAsSent() {
		when(auditService.alreadySent(any())).thenReturn(true);
		assertTrue(mailService.sendAppointmentReminderEmail("ada@kittyp.test", "Ada", "Clinic", "Miso",
				"1 Oct 2026, 11:00 AM", "Ravi", "book-1", "999", "Pune", ""));
		verify(sender, never()).sendEmail(any());
	}

	@Test
	void petConsentFallsBackToSignupOtpWhenBlank() {
		when(env.getProperty(TemplateConstant.ZEPTO_CLINIC_PET_CONSENT_OTP_EMAIL_TEMPLATE_ID)).thenReturn("");
		when(env.getProperty(TemplateConstant.ZEPTO_SIGNUP_OTP_EMAIL_TEMPLATE_ID)).thenReturn("signup-key");
		mailService.sendClinicPetConsentOtpEmail("ada@kittyp.test", "Ada", "Clinic", "Miso", "654321");
		assertEquals("signup-key", sent().getTemplateKey());
	}

	@Test
	void crmWelcomeFallsBackToParentWelcomeWhenBlank() {
		when(env.getProperty(TemplateConstant.ZEPTO_CLINIC_PARENT_CRM_WELCOME_EMAIL_TEMPLATE_ID)).thenReturn("");
		when(env.getProperty(TemplateConstant.ZOHO_PARENT_WELCOME_EMAIL_TEMPLATE_ID)).thenReturn("parent-key");
		mailService.sendClinicParentCrmWelcomeEmail("Ada", "ada@kittyp.test", "Clinic");
		assertEquals("parent-key", sent().getTemplateKey());
	}

	@Test
	void staffWelcomeFallsBackToClinicAdminWelcomeWhenBlank() {
		when(env.getProperty(TemplateConstant.ZEPTO_CLINIC_STAFF_WELCOME_EMAIL_TEMPLATE_ID)).thenReturn("");
		when(env.getProperty(TemplateConstant.ZOHO_CLINIC_ADMIN_WELCOME_EMAIL_TEMPLATE_ID)).thenReturn("admin-key");
		mailService.sendWelcomeEmailforClinicStaff("Ada", "ada@kittyp.test", "Clinic");
		assertEquals("admin-key", sent().getTemplateKey());
	}

	@Test
	void blankInviteReminderDoesNotSend() {
		when(env.getProperty(TemplateConstant.ZEPTO_CLINIC_DOCTOR_INVITE_REMINDER_EMAIL_TEMPLATE_ID)).thenReturn("");
		mailService.sendClinicDoctorInviteReminderEmail("ada@kittyp.test", "Ravi", "Clinic", "https://kittyp.in/accept");
		verify(sender, never()).sendEmail(any());
	}

	private void assertSent(String property, String mergeKey, String mergeValue) {
		ZeptoMailDto mail = sent();
		assertEquals("key:" + property, mail.getTemplateKey());
		assertEquals(mergeValue, mail.getMergeInfo().get(mergeKey));
	}

	private ZeptoMailDto sent() {
		ArgumentCaptor<ZeptoMailDto> captor = ArgumentCaptor.forClass(ZeptoMailDto.class);
		verify(sender).sendEmail(captor.capture());
		return captor.getValue();
	}

	private static User user() {
		User user = User.builder().email("ada@kittyp.test").firstName("Ada").build();
		user.setUuid("user-1");
		return user;
	}

	private static Order order() {
		Taxes taxes = new Taxes();
		taxes.setOtherTax(BigDecimal.ONE);
		taxes.setServiceCharge(BigDecimal.ONE);
		taxes.setShippingCharges(BigDecimal.ONE);
		AddressInfo address = new AddressInfo();
		address.setFormattedAddress("12 Park Road, Pune");
		return Order.builder()
				.orderNumber("ORD-1")
				.totalAmount(new BigDecimal("10.00"))
				.subTotal(new BigDecimal("8.00"))
				.taxes(taxes)
				.billingAddress(address)
				.shippingAddress(address)
				.orderItems(List.of())
				.build();
	}
}
