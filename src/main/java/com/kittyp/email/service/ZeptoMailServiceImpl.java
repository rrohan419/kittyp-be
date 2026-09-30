/**
 * @author rrohan419@gmail.com
 */
package com.kittyp.email.service;

import java.util.ArrayList;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import org.springframework.core.env.Environment;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import org.springframework.transaction.annotation.Transactional;

import com.kittyp.common.constants.AppConstant;
import com.kittyp.common.constants.TemplateConstant;
import com.kittyp.common.util.PiiMasker;
import com.kittyp.common.util.VerificationCodeService;
import com.kittyp.email.dto.EmailAttachment;
import com.kittyp.email.dto.EmailAuditDto;
import com.kittyp.email.dto.ZeptoMailDto;
import com.kittyp.email.emailsender.ZeptoMailSender;
import com.kittyp.email.model.ZeptoMailResponseModel;
import com.kittyp.order.dao.OrderDao;
import com.kittyp.order.entity.Order;
import com.kittyp.order.entity.OrderItem;
import com.kittyp.product.entity.Product;
import com.kittyp.user.dao.UserDao;
import com.kittyp.user.entity.User;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * @author rrohan419@gmail.com
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ZeptoMailServiceImpl implements ZeptoMailService {

	private final ZeptoMailSender zeptoMailSender;
	private final EmailAuditService emailAuditService;
	private final UserDao userDao;
	private final VerificationCodeService verificationCodeService;
	private final OrderDao orderDao;
	private final Environment env;

	/**
	 * @author rrohan419@gmail.com
	 */
	@Async
	@Override
	public void sendWelcomeEmailforParent(String firstName, String recipientEmail) {
		try {
			ZeptoMailDto mailDto = new ZeptoMailDto();
			mailDto.setMergeInfo(Map.of("Customer_Name", firstName));
			mailDto.setRecipientEmail(recipientEmail);
			mailDto.setRecipientName(firstName);
			if (dispatch(mailDto, TemplateConstant.ZOHO_PARENT_WELCOME_EMAIL_TEMPLATE_ID)) {
				log.info("welcome email sent for email : {}", recipientEmail);
			}
		} catch (Exception e) {
			log.warn("Failed to send parent welcome to {}: {}", recipientEmail, e.getMessage());
		}
	}

	@Override
	@Async
	public void sendWelcomeEmailforDoctor(String recipientEmail) {
		try {
			User user = userDao.userByEmail(recipientEmail);
			ZeptoMailDto mailDto = new ZeptoMailDto();
			mailDto.setMergeInfo(
					Map.of("Customer_Name", user.getFirstName(), "logo_url", AppConstant.KITTYP_EMAIL_TEMPLATE_LOGO));
			mailDto.setRecipientEmail(recipientEmail);
			mailDto.setRecipientName(user.getFirstName());
			mailDto.setTemplateKey(env.getProperty(TemplateConstant.ZOHO_DOCTOR_WELCOME_EMAIL_TEMPLATE_ID));

			ZeptoMailResponseModel responseModel = zeptoMailSender.sendEmail(mailDto);
			log.info("welcome email sent for email : " + recipientEmail);
			addEmailAuditLog(responseModel, recipientEmail);
		} catch (Exception e) {
			log.warn("Failed to send doctor welcome to {}: {}", recipientEmail, e.getMessage());
		}
	}

	@Override
	@Async
	public void sendWelcomeEmailforClinicAdmin(String recipientEmail) {
		User user = userDao.userByEmail(recipientEmail);
		ZeptoMailDto mailDto = new ZeptoMailDto();
		mailDto.setMergeInfo(Map.of("Customer_Name", user.getFirstName()));
		mailDto.setRecipientEmail(recipientEmail);
		mailDto.setRecipientName(user.getFirstName());
		try {
			if (dispatch(mailDto, TemplateConstant.ZOHO_CLINIC_ADMIN_WELCOME_EMAIL_TEMPLATE_ID)) {
				log.info("welcome email sent for email : {}", recipientEmail);
			}
		} catch (Exception e) {
			log.warn("Failed to send clinic admin welcome to {}: {}", recipientEmail, e.getMessage());
		}
	}

	/**
	 * @author rrohan419@gmail.com
	 */
	@Override
	public void sendPasswordResetCode(String email) {
		User user = userDao.userByEmail(email);
		String code = verificationCodeService.generateCode(user.getUuid());
		System.out.println("code = " + code);

		ZeptoMailDto mailDto = new ZeptoMailDto();
		mailDto.setMergeInfo(Map.of("customer_name", user.getFirstName(), "reset_code",
				code));
		mailDto.setRecipientEmail(email);
		mailDto.setRecipientName(user.getFirstName());
		mailDto.setTemplateKey(env.getProperty(TemplateConstant.ZEPTO_RESET_PASSWORD_CODE_EMAIL_TEMPLATE_ID));

		try {
			ZeptoMailResponseModel responseModel = zeptoMailSender.sendEmail(mailDto);
			log.info("password reset code sent for email : " + email);
			addEmailAuditLog(responseModel, email);
		} catch (Exception e) {
			log.warn("Failed to send password reset email to {}: {}", email, e.getMessage());
		}

	}

	@Override
	public void sendSignupOtpEmail(String recipientEmail, String code, String purpose, String phoneHint) {
		log.info("Signup OTP [{}] requested for email={} phoneHint={}", purpose, recipientEmail, phoneHint);
		try {
			ZeptoMailDto mailDto = new ZeptoMailDto();
			String name = "PHONE".equalsIgnoreCase(purpose) && phoneHint != null
					? "Phone verify (" + phoneHint + ")"
					: "Kittyp Applicant";
			mailDto.setMergeInfo(Map.of(
					"Customer_Name", name,
					"OTP", code));
			mailDto.setRecipientEmail(recipientEmail);
			mailDto.setRecipientName(name);
			mailDto.setTemplateKey(env.getProperty(TemplateConstant.ZEPTO_SIGNUP_OTP_EMAIL_TEMPLATE_ID));
			ZeptoMailResponseModel responseModel = zeptoMailSender.sendEmail(mailDto);
			addEmailAuditLog(responseModel, recipientEmail);
		} catch (Exception e) {
			// OTP is still in cache / logs — don't fail signup OTP in local if mail
			// provider is down
			log.warn("Failed to send signup OTP email to {}: {}", recipientEmail, e.getMessage());
		}
	}

	@Override
	public void sendClinicPetConsentOtpEmail(String recipientEmail, String ownerName, String clinicName, String petName,
			String code) {
		String clinic = clinicName == null || clinicName.isBlank() ? "Clinic" : clinicName.trim();
		String pet = petName == null || petName.isBlank() ? "pet" : petName.trim();
		String name = ownerName == null || ownerName.isBlank() ? "Pet parent" : ownerName.trim();
		log.info("Clinic pet-consent OTP to email={} clinic={} pet={}", recipientEmail, clinic, pet);
		try {
			ZeptoMailDto mailDto = new ZeptoMailDto();
			mailDto.setMergeInfo(Map.of(
					"customer_name", name,
					"otp", code,
					"clinic_name", clinic,
					"pet_name", pet));
			mailDto.setRecipientEmail(recipientEmail);
			mailDto.setRecipientName(name);
			String templateKey = env.getProperty(TemplateConstant.ZEPTO_CLINIC_PET_CONSENT_OTP_EMAIL_TEMPLATE_ID);
			if (templateKey == null || templateKey.isBlank()) {
				templateKey = env.getProperty(TemplateConstant.ZEPTO_SIGNUP_OTP_EMAIL_TEMPLATE_ID);
			}
			mailDto.setTemplateKey(templateKey);
			ZeptoMailResponseModel responseModel = zeptoMailSender.sendEmail(mailDto);
			addEmailAuditLog(responseModel, recipientEmail);
		} catch (Exception e) {
			log.warn("Failed to send clinic pet-consent OTP to {}: {} (OTP remains in cache)", recipientEmail,
					e.getMessage());
		}
	}

	@Override
	public void sendClinicClientAttachOtpEmail(String recipientEmail, String ownerName, String clinicName,
			String code) {
		String clinic = clinicName == null || clinicName.isBlank() ? "Clinic" : clinicName.trim();
		String name = ownerName == null || ownerName.isBlank() ? "Pet parent" : ownerName.trim();
		log.info("Clinic pet-consent OTP to email={} clinic={}", recipientEmail, clinic);
		try {
			ZeptoMailDto mailDto = new ZeptoMailDto();
			mailDto.setMergeInfo(Map.of(
					"customer_name", name,
					"otp", code,
					"clinic_name", clinic));
			mailDto.setRecipientEmail(recipientEmail);
			mailDto.setRecipientName(name);
			String templateKey = env.getProperty(TemplateConstant.ZEPTO_CLINIC_CLIENT_CONSENT_EMAIL_TEMPLATE_ID);

			mailDto.setTemplateKey(templateKey);
			ZeptoMailResponseModel responseModel = zeptoMailSender.sendEmail(mailDto);
			addEmailAuditLog(responseModel, recipientEmail);
		} catch (Exception e) {
			log.warn("Failed to send clinic pet-consent OTP to {}: {} (OTP remains in cache)", recipientEmail,
					e.getMessage());
		}
	}

	@Override
	public void sendClinicDoctorInviteEmail(String recipientEmail, String doctorName, String clinicName,
			String acceptUrl) {
		log.info("Clinic doctor invite to email={} clinic={} acceptUrl={}", recipientEmail, clinicName, acceptUrl);
		try {
			ZeptoMailDto mailDto = new ZeptoMailDto();
			String name = doctorName == null || doctorName.isBlank() ? "Doctor" : doctorName;
			mailDto.setMergeInfo(Map.of(
					"doctor_name", name,
					"clinic_name", clinicName,
					"acceptUrl", acceptUrl));
			mailDto.setRecipientEmail(recipientEmail);
			mailDto.setRecipientName(name);
			mailDto.setTemplateKey(env.getProperty(TemplateConstant.ZEPTO_CLINIC_DOCTOR_INVITE_EMAIL_TEMPLATE_ID));
			ZeptoMailResponseModel responseModel = zeptoMailSender.sendEmail(mailDto);
			addEmailAuditLog(responseModel, recipientEmail);
		} catch (Exception e) {
			log.warn("Failed to send clinic invite email to {}: {} (acceptUrl logged above)", recipientEmail,
					e.getMessage());
		}
	}

	@Override
	public void sendClinicStaffInviteEmail(String recipientEmail, String staffName, String clinicName,
			String acceptUrl) {
		log.info("Clinic staff invite to email={} clinic={} acceptUrl={}", recipientEmail, clinicName, acceptUrl);
		try {
			ZeptoMailDto mailDto = new ZeptoMailDto();
			String name = staffName == null || staffName.isBlank() ? "Staff" : staffName;
			mailDto.setMergeInfo(Map.of(
					"clinic_name", clinicName,
					"acceptUrl", acceptUrl,
					"staff_name", name));
			mailDto.setRecipientEmail(recipientEmail);
			mailDto.setRecipientName(name);
			mailDto.setTemplateKey(env.getProperty(TemplateConstant.ZEPTO_CLINIC_STAFF_INVITE_EMAIL_TEMPLATE_ID));
			ZeptoMailResponseModel responseModel = zeptoMailSender.sendEmail(mailDto);
			addEmailAuditLog(responseModel, recipientEmail);
		} catch (Exception e) {
			log.warn("Failed to send clinic staff invite email to {}: {} (acceptUrl logged above)", recipientEmail,
					e.getMessage());
		}
	}

	@Override
	public void sendClinicDoctorInviteReminderEmail(String recipientEmail, String doctorName, String clinicName,
			String acceptUrl) {
		log.info("Clinic doctor invite REMINDER to email={} clinic={} acceptUrl={}", recipientEmail, clinicName,
				acceptUrl);
		if (recipientEmail == null || recipientEmail.isBlank()) {
			return;
		}
		String name = doctorName == null || doctorName.isBlank() ? "Doctor" : doctorName;
		ZeptoMailDto mailDto = new ZeptoMailDto();
		mailDto.setMergeInfo(Map.of(
				"doctor_name", name,
				"clinic_name", clinicName == null ? "" : clinicName,
				"acceptUrl", acceptUrl == null ? "" : acceptUrl));
		mailDto.setRecipientEmail(recipientEmail);
		mailDto.setRecipientName(name);
		dispatch(mailDto, TemplateConstant.ZEPTO_CLINIC_DOCTOR_INVITE_REMINDER_EMAIL_TEMPLATE_ID);
	}

	@Override
	public void sendClinicDoctorInviteResponseEmail(String recipientEmail, String clinicName, String doctorName,
			String doctorEmail, boolean accepted) {
		String status = accepted ? "accepted" : "declined";
		log.info("Clinic invite {} — notify clinicEmail={} clinic={} doctor={} <{}>", status, recipientEmail,
				clinicName, doctorName, doctorEmail);
		if (recipientEmail == null || recipientEmail.isBlank()) {
			return;
		}
		try {
			ZeptoMailDto mailDto = new ZeptoMailDto();
			String name = clinicName == null || clinicName.isBlank() ? "Clinic" : clinicName;

			mailDto.setMergeInfo(Map.of(
					"doctor_name", doctorName,
					"doctor_email", doctorEmail,
					"clinic_name", name,
					"status", status));
			mailDto.setRecipientEmail(recipientEmail);
			mailDto.setRecipientName(name);
			mailDto.setTemplateKey(
					env.getProperty(TemplateConstant.ZEPTO_CLINIC_DOCTOR_INVITE_RESPONSE_EMAIL_TEMPLATE_ID));
			ZeptoMailResponseModel responseModel = zeptoMailSender.sendEmail(mailDto);
			addEmailAuditLog(responseModel, recipientEmail);
		} catch (Exception e) {
			log.warn("Failed to send clinic invite response email to {}: {}", recipientEmail, e.getMessage());
		}
	}

	@Transactional
	@Override
	public void sendOrderConfirmationEmail(String recipientEmail, String orderNumber) {
		User user = userDao.userByEmail(recipientEmail);
		Order order = orderDao.orderByOrderNumber(orderNumber);

		ZeptoMailDto mailDto = new ZeptoMailDto();
		mailDto.setRecipientEmail(recipientEmail);
		mailDto.setRecipientName(user.getFirstName());
		mailDto.setTemplateKey(env.getProperty(TemplateConstant.ZEPTO_ORDER_CONFIRMATION_EMAIL_TEMPLATE_ID));

		// Create the products array
		List<Map<String, Object>> productsList = new ArrayList<>();

		for (OrderItem orderItem : order.getOrderItems()) {
			Product productEntity = orderItem.getProduct();
			Map<String, Object> product = new HashMap<>();

			product.put("quantity", String.valueOf(orderItem.getQuantity()));
			product.put("name", productEntity.getName());
			product.put("price", String.valueOf(productEntity.getPrice()));

			// Add image URL
			Set<String> imageUrls = productEntity.getProductImageUrls();
			product.put("image_url", imageUrls.stream().findFirst().orElse(""));

			// Add color and size as nested objects (not under "this")
			if (productEntity.getAttributes() != null) {
				if (productEntity.getAttributes().getColor() != null
						&& !productEntity.getAttributes().getColor().isEmpty()) {
					product.put("color", Map.of("color", productEntity.getAttributes().getColor()));
				}
				if (productEntity.getAttributes().getSize() != null
						&& !productEntity.getAttributes().getSize().isEmpty()) {
					product.put("size", Map.of("size", productEntity.getAttributes().getSize()));
				}
				if (productEntity.getAttributes().getMaterial() != null
						&& !productEntity.getAttributes().getMaterial().isEmpty()) {
					product.put("material", Map.of("material", productEntity.getAttributes().getMaterial()));
				}
			}

			productsList.add(product);
		}

		// Create the root map with all required fields
		Map<String, Object> root = new HashMap<>();
		root.put("facebook_url", AppConstant.KITTYP_FACEBOOK_URL);
		root.put("tracking_url", "tracking_url_value");
		root.put("twitter_url", "twitter_url_value");
		root.put("order_number", order.getOrderNumber());
		root.put("tax", order.getTaxes().getOtherTax().add(order.getTaxes().getServiceCharge()).toString());
		root.put("billing_address", order.getBillingAddress().getFormattedAddress());
		root.put("products", productsList); // Pass the list directly
		root.put("total", order.getTotalAmount().toString());
		root.put("shipping", order.getTaxes().getShippingCharges().toString());
		root.put("instagram_url", AppConstant.KITTYP_INSTAGRAM_URL);
		root.put("subtotal", order.getSubTotal().toString());
		root.put("customer_name", user.getFirstName());
		root.put("shipping_address", order.getShippingAddress().getFormattedAddress());

		// Set merge info directly as a map (no JSON serialization/deserialization)
		mailDto.setMergeInfo(root);
		log.info("ZeptoMail Merge Info: {}", root);

		ZeptoMailResponseModel responseModel = zeptoMailSender.sendEmail(mailDto);
		log.info("Order confirmation email sent to: {}", recipientEmail);
		addEmailAuditLog(responseModel, recipientEmail);
	}

	private void addEmailAuditLog(ZeptoMailResponseModel responseModel, String recipientEmail) {
		addEmailAuditLog(responseModel, recipientEmail, "email_Sent");
	}

	private void addEmailAuditLog(ZeptoMailResponseModel responseModel, String recipientEmail, String eventName) {
		EmailAuditDto emailAudit = new EmailAuditDto();
		emailAudit.setRecipientEmail(recipientEmail);
		emailAudit.setMessage(responseModel.getMessage());
		emailAudit.setMessageStatus(responseModel.getData().get(0).getMessage());
		emailAudit.setStatusCode(responseModel.getData().get(0).getCode());
		emailAudit.setRequestId(responseModel.getRequestId());
		emailAudit.setProvider("Zepto Mail");
		emailAudit.setEventName(eventName);

		emailAuditService.saveEmailAudit(emailAudit);
		log.info("email audit added for email: " + recipientEmail + " request id : " + responseModel.getRequestId());
	}

	@Override
	public void sendDoctorProfileVerified(String email, String doctorName, String dashboardUrl) {
		String name = blankToDefault(doctorName, "Doctor");
		sendDedicated(email, name, TemplateConstant.ZOHO_DOCTOR_PROFILE_VERIFIED_TEMPLATE_ID, Map.of(
				"customer_name", name,
				"doctor_url", blankToDefault(dashboardUrl, "")));
	}

	@Override
	public void sendClinicProfileVerified(String email, String customerName, String clinicName, String clinicUrl) {
		String name = blankToDefault(customerName, "there");
		sendDedicated(email, name, TemplateConstant.ZOHO_CLINIC_PROFILE_VERIFIED_TEMPLATE_ID, Map.of(
				"customer_name", name,
				"clinic_name", blankToDefault(clinicName, "Clinic"),
				"clinic_url", blankToDefault(clinicUrl, "")));
	}

	@Override
	public void sendInvoiceEmail(String email, String customerName, String clinicName, String petName,
			String invoiceNumber, String amount, String invoiceUrl, byte[] pdfBytes, String filename) {
		if (email == null || email.isBlank()) {
			log.warn("Skipping invoice email: recipient is blank");
			return;
		}
		String name = blankToDefault(customerName, "there");
		ZeptoMailDto mailDto = new ZeptoMailDto();
		mailDto.setMergeInfo(Map.of(
				"customer_name", name,
				"clinic_name", blankToDefault(clinicName, "KittyP Clinic"),
				"pet_name", blankToDefault(petName, "your pet"),
				"invoice_number", blankToDefault(invoiceNumber, ""),
				"amount", blankToDefault(amount, "0.00"),
				"invoice_url", blankToDefault(invoiceUrl, "")));
		mailDto.setRecipientEmail(email.trim());
		mailDto.setRecipientName(name);
		if (pdfBytes != null && pdfBytes.length > 0) {
			String attachName = filename == null || filename.isBlank() ? "invoice.pdf" : filename;
			if (!attachName.toLowerCase().endsWith(".pdf")) {
				attachName = attachName + ".pdf";
			}
			mailDto.setAttachments(List.of(new EmailAttachment(
					Base64.getEncoder().encodeToString(pdfBytes),
					"application/pdf",
					attachName)));
		}
		try {
			if (!dispatch(mailDto, TemplateConstant.ZOHO_TREATMENT_INVOICE_EMAIL_TEMPLATE_ID)) {
				throw new IllegalStateException("Invoice email template is not configured");
			}
			log.info("Invoice email sent to {} invoice={}", PiiMasker.maskEmail(email), invoiceNumber);
		} catch (Exception e) {
			log.warn("Failed to send invoice email to {}: {}", PiiMasker.maskEmail(email), e.getMessage());
			if (e instanceof RuntimeException re) {
				throw re;
			}
			throw new IllegalStateException(e.getMessage(), e);
		}
	}

	private static String blankToDefault(String value, String fallback) {
		return value == null || value.isBlank() ? fallback : value;
	}

	private static void putIfPresent(Map<String, Object> merge, String key, String value) {
		if (value != null && !value.isBlank()) {
			merge.put(key, value);
		}
	}

	private void sendDedicated(String recipientEmail, String recipientName, String templateProperty,
			Map<String, Object> mergeInfo) {
		if (recipientEmail == null || recipientEmail.isBlank()) {
			return;
		}
		ZeptoMailDto mailDto = new ZeptoMailDto();
		mailDto.setMergeInfo(mergeInfo);
		mailDto.setRecipientEmail(recipientEmail.trim());
		mailDto.setRecipientName(recipientName);
		try {
			if (dispatch(mailDto, templateProperty)) {
				log.info("Dedicated template email sent to {} using {}", PiiMasker.maskEmail(recipientEmail),
						templateProperty);
			}
		} catch (Exception e) {
			log.warn("Failed to send dedicated template email to {}: {}", PiiMasker.maskEmail(recipientEmail),
					e.getMessage());
		}
	}

	@Override
	@Async
	public void sendPhoneChangedNotification(String email, String firstName, String clinicName, String phone,
			String loginUrl) {
		String name = blankToDefault(firstName, "there");
		sendDedicated(email, name, TemplateConstant.ZEPTO_PHONE_CHANGED_EMAIL_TEMPLATE_ID, Map.of(
				"Customer_Name", name,
				"Clinic_Name", blankToDefault(clinicName, "KittyP"),
				"phone", blankToDefault(phone, ""),
				"login_url", blankToDefault(loginUrl, "")));
	}

	@Override
	public void sendEmailChangeOtp(String recipientEmail, String firstName, String clinicName, String code) {
		String name = blankToDefault(firstName, "there");
		sendDedicated(recipientEmail, name, TemplateConstant.ZEPTO_EMAIL_CHANGE_OTP_TEMPLATE_ID, Map.of(
				"Customer_Name", name,
				"Clinic_Name", blankToDefault(clinicName, "KittyP"),
				"OTP", blankToDefault(code, "")));
	}

	@Override
	@Async
	public void sendPasswordChangedNotification(String email, String firstName, String clinicName, String changedAt) {
		String name = blankToDefault(firstName, "there");
		sendDedicated(email, name, TemplateConstant.ZEPTO_PASSWORD_CHANGED_EMAIL_TEMPLATE_ID, Map.of(
				"Customer_Name", name,
				"Clinic_Name", blankToDefault(clinicName, "KittyP"),
				"changed_at", blankToDefault(changedAt, "")));
	}

	@Override
	@Async
	public void sendClinicParentCrmWelcomeEmail(String firstName, String recipientEmail, String clinicName) {
		if (recipientEmail == null || recipientEmail.isBlank()) {
			return;
		}
		String name = blankToDefault(firstName, "there");
		ZeptoMailDto mailDto = new ZeptoMailDto();
		mailDto.setMergeInfo(Map.of(
				"Customer_Name", name,
				"Clinic_Name", blankToDefault(clinicName, "Clinic"),
				"clinic_name", blankToDefault(clinicName, "Clinic")));
		mailDto.setRecipientEmail(recipientEmail.trim());
		mailDto.setRecipientName(name);
		String key = TemplateConstant.ZEPTO_CLINIC_PARENT_CRM_WELCOME_EMAIL_TEMPLATE_ID;
		if (!dispatch(mailDto, key)) {
			dispatch(mailDto, TemplateConstant.ZOHO_PARENT_WELCOME_EMAIL_TEMPLATE_ID);
		}
	}

	@Override
	@Async
	public void sendAppointmentConfirmationEmail(String recipientEmail, String ownerName, String clinicName,
			String petName, String when, String doctorName, String bookingId, String clinicAddress, String mapsUrl,
			String rescheduleUrl, String cancelUrl) {
		sendAppointmentTemplate(recipientEmail, ownerName, clinicName, petName, when, doctorName, bookingId, null,
				clinicAddress, mapsUrl, rescheduleUrl, cancelUrl, null, null, null,
				TemplateConstant.ZEPTO_APPOINTMENT_CONFIRMATION_EMAIL_TEMPLATE_ID,
				"appointment_confirmation:" + blankToDefault(bookingId, ""));
	}

	@Override
	public boolean sendAppointmentReminderEmail(String recipientEmail, String ownerName, String clinicName,
			String petName, String when, String doctorName, String bookingId, String clinicPhone,
			String clinicAddress, String mapsUrl) {
		return sendAppointmentTemplate(recipientEmail, ownerName, clinicName, petName, when, doctorName, bookingId,
				clinicPhone, clinicAddress, mapsUrl, null, null, null, null, null,
				TemplateConstant.ZEPTO_APPOINTMENT_REMINDER_EMAIL_TEMPLATE_ID,
				"appointment_reminder:" + blankToDefault(bookingId, "") + ":" + blankToDefault(when, ""));
	}

	@Override
	@Async
	public void sendAppointmentRescheduledEmail(String recipientEmail, String ownerName, String clinicName,
			String petName, String when, String doctorName, String bookingId, String clinicAddress, String mapsUrl,
			String previousWhen, String clinicPhone, String manageUrl) {
		sendAppointmentTemplate(recipientEmail, ownerName, clinicName, petName, when, doctorName, bookingId,
				clinicPhone, clinicAddress, mapsUrl, null, null, previousWhen, null, manageUrl,
				TemplateConstant.ZEPTO_APPOINTMENT_RESCHEDULED_EMAIL_TEMPLATE_ID,
				"appointment_rescheduled:" + blankToDefault(bookingId, "") + ":" + blankToDefault(when, ""));
	}

	@Override
	@Async
	public void sendAppointmentCancelledEmail(String recipientEmail, String ownerName, String clinicName,
			String petName, String when, String doctorName, String bookingId, String clinicPhone,
			String clinicAddress, String bookUrl) {
		sendAppointmentTemplate(recipientEmail, ownerName, clinicName, petName, when, doctorName, bookingId,
				clinicPhone, clinicAddress, null, null, null, null, bookUrl, null,
				TemplateConstant.ZEPTO_APPOINTMENT_CANCELLED_EMAIL_TEMPLATE_ID,
				"appointment_cancelled:" + blankToDefault(bookingId, ""));
	}

	@Override
	@Async
	public void sendWalkInCheckedInEmail(String recipientEmail, String ownerName, String clinicName, String petName,
			String doctorName, String visitId, String clinicPhone, String clinicAddress, String mapsUrl) {
		sendAppointmentTemplate(recipientEmail, ownerName, clinicName, petName, "", doctorName, visitId,
				clinicPhone, clinicAddress, mapsUrl, null, null, null, null, null,
				TemplateConstant.ZEPTO_WALKIN_CHECKED_IN_EMAIL_TEMPLATE_ID,
				"walk_in_checked_in:" + blankToDefault(visitId, ""));
	}

	@Override
	@Async
	public void sendAppointmentDoctorChangedEmail(String recipientEmail, String ownerName, String clinicName,
			String petName, String when, String doctorName, String bookingId, String doctorUuid, String clinicAddress,
			String mapsUrl, String clinicPhone, String manageUrl) {
		sendAppointmentTemplate(recipientEmail, ownerName, clinicName, petName, when, doctorName, bookingId,
				clinicPhone, clinicAddress, mapsUrl, null, null, null, null, manageUrl,
				TemplateConstant.ZEPTO_APPOINTMENT_DOCTOR_CHANGED_EMAIL_TEMPLATE_ID,
				"appointment_doctor_changed:" + blankToDefault(bookingId, "") + ":" + blankToDefault(doctorUuid, ""));
	}

	@Override
	@Async
	public void sendClinicClosureEmail(String recipientEmail, String ownerName, String clinicName, String petName,
			String when, String doctorName, String bookingId, String clinicPhone) {
		sendAppointmentTemplate(recipientEmail, ownerName, clinicName, petName, when, doctorName, bookingId,
				clinicPhone, "", "", null, null, null, null, null,
				TemplateConstant.ZEPTO_CLINIC_CLOSURE_EMAIL_TEMPLATE_ID,
				"clinic_closure:" + blankToDefault(bookingId, ""));
	}

	@Override
	@Async
	public void sendInviteRevokedEmail(String recipientEmail, String inviteeName, String clinicName, String inviteRole,
			String inviteUuid) {
		if (recipientEmail == null || recipientEmail.isBlank()) {
			return;
		}
		String eventName = "invite_revoked:" + blankToDefault(inviteUuid, "");
		if (emailAuditService.alreadySent(eventName)) {
			log.info("Skipping duplicate invite revoked email {}", eventName);
			return;
		}
		String name = blankToDefault(inviteeName, "there");
		String clinic = blankToDefault(clinicName, "Clinic");
		Map<String, Object> merge = new HashMap<>();
		merge.put("Customer_Name", name);
		merge.put("Clinic_Name", clinic);
		merge.put("clinic_name", clinic);
		merge.put("invite_role", blankToDefault(inviteRole, "member"));
		ZeptoMailDto mailDto = new ZeptoMailDto();
		mailDto.setMergeInfo(merge);
		mailDto.setRecipientEmail(recipientEmail.trim());
		mailDto.setRecipientName(name);
		dispatchWithRetry(mailDto, TemplateConstant.ZEPTO_INVITE_REVOKED_EMAIL_TEMPLATE_ID, eventName);
	}

	@Override
	@Async
	public void sendClinicLocationChangedEmail(String recipientEmail, String recipientName, String clinicName,
			String moveDate, String oldLocation, String newLocation, String oldMapsUrl, String newMapsUrl,
			String clinicUuid, String eventDate) {
		if (recipientEmail == null || recipientEmail.isBlank()) {
			return;
		}
		String eventName = "clinic_location_changed:" + blankToDefault(clinicUuid, "") + ":"
				+ blankToDefault(eventDate, "") + ":" + recipientEmail.trim().toLowerCase(Locale.ROOT);
		if (emailAuditService.alreadySent(eventName)) {
			log.info("Skipping duplicate clinic location email {}", eventName);
			return;
		}
		String name = blankToDefault(recipientName, "there");
		String clinic = blankToDefault(clinicName, "Clinic");
		Map<String, Object> merge = new HashMap<>();
		merge.put("Customer_Name", name);
		merge.put("Clinic_Name", clinic);
		merge.put("clinic_name", clinic);
		merge.put("move_date", blankToDefault(moveDate, ""));
		merge.put("old_location", blankToDefault(oldLocation, ""));
		merge.put("new_location", blankToDefault(newLocation, ""));
		merge.put("old_maps_url", blankToDefault(oldMapsUrl, ""));
		merge.put("new_maps_url", blankToDefault(newMapsUrl, ""));
		ZeptoMailDto mailDto = new ZeptoMailDto();
		mailDto.setMergeInfo(merge);
		mailDto.setRecipientEmail(recipientEmail.trim());
		mailDto.setRecipientName(name);
		dispatchWithRetry(mailDto, TemplateConstant.ZEPTO_CLINIC_LOCATION_CHANGED_EMAIL_TEMPLATE_ID, eventName);
	}

	private boolean sendAppointmentTemplate(String recipientEmail, String ownerName, String clinicName, String petName,
			String when, String doctorName, String bookingId, String clinicPhone, String clinicAddress, String mapsUrl,
			String rescheduleUrl, String cancelUrl, String previousWhen, String bookUrl, String manageUrl,
			String templateProperty, String eventName) {
		if (recipientEmail == null || recipientEmail.isBlank()) {
			return false;
		}
		if (emailAuditService.alreadySent(eventName)) {
			log.info("Skipping duplicate appointment email {}", eventName);
			return true;
		}
		String name = blankToDefault(ownerName, "there");
		Map<String, Object> merge = new HashMap<>();
		merge.put("Customer_Name", name);
		merge.put("Clinic_Name", blankToDefault(clinicName, "Clinic"));
		merge.put("clinic_name", blankToDefault(clinicName, "Clinic"));
		merge.put("pet_name", blankToDefault(petName, "your pet"));
		merge.put("appointment_when", blankToDefault(when, "soon"));
		merge.put("doctor_name", blankToDefault(doctorName, "your veterinarian"));
		merge.put("booking_id", blankToDefault(bookingId, ""));
		merge.put("clinic_address", blankToDefault(clinicAddress, ""));
		merge.put("maps_url", blankToDefault(mapsUrl, ""));
		if (clinicPhone != null) {
			merge.put("clinic_phone", clinicPhone);
		}
		putIfPresent(merge, "reschedule_url", rescheduleUrl);
		putIfPresent(merge, "cancel_url", cancelUrl);
		putIfPresent(merge, "previous_appointment_when", previousWhen);
		putIfPresent(merge, "book_url", bookUrl);
		putIfPresent(merge, "manage_url", manageUrl);
		ZeptoMailDto mailDto = new ZeptoMailDto();
		mailDto.setMergeInfo(merge);
		mailDto.setRecipientEmail(recipientEmail.trim());
		mailDto.setRecipientName(name);
		return dispatchWithRetry(mailDto, templateProperty, eventName);
	}

	@Override
	@Async
	public void sendWelcomeEmailforClinicStaff(String firstName, String recipientEmail, String clinicName) {
		if (recipientEmail == null || recipientEmail.isBlank()) {
			return;
		}
		String name = blankToDefault(firstName, "there");
		ZeptoMailDto mailDto = new ZeptoMailDto();
		mailDto.setMergeInfo(Map.of(
				"Customer_Name", name,
				"Clinic_Name", blankToDefault(clinicName, "Clinic"),
				"clinic_name", blankToDefault(clinicName, "Clinic")));
		mailDto.setRecipientEmail(recipientEmail.trim());
		mailDto.setRecipientName(name);
		String key = TemplateConstant.ZEPTO_CLINIC_STAFF_WELCOME_EMAIL_TEMPLATE_ID;
		if (!dispatch(mailDto, key)) {
			dispatch(mailDto, TemplateConstant.ZOHO_CLINIC_ADMIN_WELCOME_EMAIL_TEMPLATE_ID);
		}
	}

	private String resolveTemplateKey(String templateProperty) {
		String templateKey = env.getProperty(templateProperty);
		if (templateKey == null || templateKey.isBlank()) {
			log.warn("Skipping email: template key not configured for {}", templateProperty);
			return null;
		}
		return templateKey.trim();
	}

	private boolean dispatch(ZeptoMailDto mailDto, String templateProperty) {
		String templateKey = resolveTemplateKey(templateProperty);
		if (templateKey == null) {
			return false;
		}
		mailDto.setTemplateKey(templateKey);
		ZeptoMailResponseModel responseModel = zeptoMailSender.sendEmail(mailDto);
		addEmailAuditLog(responseModel, mailDto.getRecipientEmail());
		return true;
	}

	/** Three attempts. Waits 1s then 2s between failures. Does not fall back to another template. */
	private boolean dispatchWithRetry(ZeptoMailDto mailDto, String templateProperty, String eventName) {
		String templateKey = resolveTemplateKey(templateProperty);
		if (templateKey == null) {
			return false;
		}
		mailDto.setTemplateKey(templateKey);
		for (int attempt = 1; attempt <= 3; attempt++) {
			try {
				ZeptoMailResponseModel responseModel = zeptoMailSender.sendEmail(mailDto);
				addEmailAuditLog(responseModel, mailDto.getRecipientEmail(), eventName);
				return true;
			} catch (RuntimeException e) {
				if (attempt == 3) {
					log.warn("Appointment email {} failed after {} attempts: {}", eventName, attempt, e.getMessage());
					return false;
				}
				try {
					Thread.sleep(1000L * attempt);
				} catch (InterruptedException interrupted) {
					Thread.currentThread().interrupt();
					return false;
				}
			}
		}
		return false;
	}
}
