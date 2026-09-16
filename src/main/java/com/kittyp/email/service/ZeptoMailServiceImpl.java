/**
 * @author rrohan419@gmail.com
 */
package com.kittyp.email.service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.springframework.core.env.Environment;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import org.springframework.transaction.annotation.Transactional;

import com.kittyp.common.constants.AppConstant;
import com.kittyp.common.constants.TemplateConstant;
import com.kittyp.common.logging.PiiMasker;
import com.kittyp.common.util.VerificationCodeService;
import com.kittyp.email.ZeptoMergeFields;
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
		// User user = userDao.userByEmail(recipientEmail);

		ZeptoMailDto mailDto = new ZeptoMailDto();
		mailDto.setMergeInfo(withLogoYear(Map.of("Customer_Name", firstName)));
		mailDto.setRecipientEmail(recipientEmail);
		mailDto.setRecipientName(firstName);
		if (!dispatch(mailDto, TemplateConstant.ZOHO_PARENT_WELCOME_EMAIL_TEMPLATE_ID)) {
			return;
		}
		log.info("welcome email sent for email : {}", PiiMasker.maskEmail(recipientEmail));

	}

	@Override
	public void sendWelcomeEmailforDoctor(String recipientEmail) {
		User user = userDao.userByEmail(recipientEmail);
		ZeptoMailDto mailDto = new ZeptoMailDto();
		mailDto.setMergeInfo(withLogoYear(Map.of("Customer_Name", user.getFirstName())));
		mailDto.setRecipientEmail(recipientEmail);
		mailDto.setRecipientName(user.getFirstName());
		if (!dispatch(mailDto, TemplateConstant.ZOHO_DOCTOR_WELCOME_EMAIL_TEMPLATE_ID)) {
			return;
		}
		log.info("welcome email sent for email : {}", PiiMasker.maskEmail(recipientEmail));
	}

	@Override
	public void sendWelcomeEmailforClinicAdmin(String recipientEmail) {
		User user = userDao.userByEmail(recipientEmail);
		ZeptoMailDto mailDto = new ZeptoMailDto();
		mailDto.setMergeInfo(withLogoYear(Map.of("Customer_Name", user.getFirstName())));
		mailDto.setRecipientEmail(recipientEmail);
		mailDto.setRecipientName(user.getFirstName());
		if (!dispatch(mailDto, TemplateConstant.ZOHO_CLINIC_ADMIN_WELCOME_EMAIL_TEMPLATE_ID)) {
			return;
		}
		log.info("welcome email sent for email : {}", PiiMasker.maskEmail(recipientEmail));
	}

	/**
	 * @author rrohan419@gmail.com
	 */
	@Override
	public void sendPasswordResetCode(String email) {
		User user = userDao.userByEmail(email);
		String code = verificationCodeService.generateCode(user.getUuid());

		ZeptoMailDto mailDto = new ZeptoMailDto();
		mailDto.setMergeInfo(withLogoYear(Map.of(
				"customer_name", user.getFirstName(),
				ZeptoMergeFields.CUSTOMER_NAME, user.getFirstName(),
				ZeptoMergeFields.RESET_CODE, code)));
		mailDto.setRecipientEmail(email);
		mailDto.setRecipientName(user.getFirstName());
		try {
			if (!dispatch(mailDto, TemplateConstant.ZEPTO_RESET_PASSWORD_CODE_EMAIL_TEMPLATE_ID)) {
				return;
			}
			log.info("password reset code sent for email : {}", PiiMasker.maskEmail(email));
		} catch (Exception e) {
			log.warn("Failed to send password reset email to {}: {}", PiiMasker.maskEmail(email), e.getMessage());
		}

	}

	@Override
	public void sendSignupOtpEmail(String recipientEmail, String code, String purpose, String phoneHint) {
		log.info("Signup OTP [{}] requested for email={} phoneHint={}", purpose, PiiMasker.maskEmail(recipientEmail),
				phoneHint);
		try {
			ZeptoMailDto mailDto = new ZeptoMailDto();
			String name;
			if ("PHONE".equalsIgnoreCase(purpose) && phoneHint != null) {
				name = "Phone verify (" + phoneHint + ")";
			} else if ("CLINIC".equalsIgnoreCase(purpose) || "CLINIC_ADMIN".equalsIgnoreCase(purpose)) {
				name = "Clinic Admin";
			} else if ("PARENT".equalsIgnoreCase(purpose) || "USER".equalsIgnoreCase(purpose)) {
				name = "Pet Parent";
			} else if ("DOCTOR".equalsIgnoreCase(purpose)) {
				name = "Doctor Applicant";
			} else {
				name = "there";
			}
			mailDto.setMergeInfo(withLogoYearTypo(Map.of(
					"Customer_Name", name,
					"OTP", code)));
			mailDto.setRecipientEmail(recipientEmail);
			mailDto.setRecipientName(name);
			dispatch(mailDto, TemplateConstant.ZEPTO_SIGNUP_OTP_EMAIL_TEMPLATE_ID);
		} catch (Exception e) {
			// OTP is still in cache / logs — don't fail signup OTP in local if mail provider is down
			log.warn("Failed to send signup OTP email to {}: {}", PiiMasker.maskEmail(recipientEmail), e.getMessage());
		}
	}

	@Override
	public void sendClinicPetConsentOtpEmail(String recipientEmail, String ownerName, String clinicName, String petName,
			String code) {
		String clinic = clinicName == null || clinicName.isBlank() ? "Clinic" : clinicName.trim();
		String pet = petName == null || petName.isBlank() ? "pet" : petName.trim();
		String name = ownerName == null || ownerName.isBlank() ? "Pet parent" : ownerName.trim();
		log.info("Clinic pet-consent OTP to email={} clinic={} pet={}", PiiMasker.maskEmail(recipientEmail), clinic,
				pet);
		try {
			ZeptoMailDto mailDto = new ZeptoMailDto();
			mailDto.setMergeInfo(withLogoYear(Map.of(
					"customer_name", name,
					ZeptoMergeFields.CUSTOMER_NAME, name,
					"otp", code,
					ZeptoMergeFields.OTP, code,
					ZeptoMergeFields.CLINIC_NAME, clinic,
					"pet_name", pet)));
			mailDto.setRecipientEmail(recipientEmail);
			mailDto.setRecipientName(name);
			String templateProperty = TemplateConstant.ZEPTO_CLINIC_PET_CONSENT_OTP_EMAIL_TEMPLATE_ID;
			String dedicatedKey = env.getProperty(templateProperty);
			if (dedicatedKey == null || dedicatedKey.isBlank()) {
				templateProperty = TemplateConstant.ZEPTO_SIGNUP_OTP_EMAIL_TEMPLATE_ID;
			}
			dispatch(mailDto, templateProperty);
		} catch (Exception e) {
			log.warn("Failed to send clinic pet-consent OTP to {}: {} (OTP remains in cache)",
					PiiMasker.maskEmail(recipientEmail), e.getMessage());
		}
	}

	@Override
	public void sendClinicDoctorInviteEmail(String recipientEmail, String doctorName, String clinicName,
			String acceptUrl) {
		log.info("Clinic doctor invite to email={} clinic={}", PiiMasker.maskEmail(recipientEmail), clinicName);
		try {
			ZeptoMailDto mailDto = new ZeptoMailDto();
			String name = doctorName == null || doctorName.isBlank() ? "Doctor" : doctorName;
			mailDto.setMergeInfo(withLogoYear(Map.of(
					"doctor_name", name,
					ZeptoMergeFields.CLINIC_NAME, clinicName,
					ZeptoMergeFields.ACCEPT_URL, acceptUrl)));
			mailDto.setRecipientEmail(recipientEmail);
			mailDto.setRecipientName(name);
			dispatch(mailDto, TemplateConstant.ZEPTO_CLINIC_DOCTOR_INVITE_EMAIL_TEMPLATE_ID);
		} catch (Exception e) {
			log.warn("Failed to send clinic invite email to {}: {}", PiiMasker.maskEmail(recipientEmail),
					e.getMessage());
		}
	}

	@Override
	public void sendClinicStaffInviteEmail(String recipientEmail, String staffName, String clinicName,
			String acceptUrl) {
		log.info("Clinic staff invite to email={} clinic={}", PiiMasker.maskEmail(recipientEmail), clinicName);
		try {
			ZeptoMailDto mailDto = new ZeptoMailDto();
			String name = staffName == null || staffName.isBlank() ? "Staff" : staffName;
			mailDto.setMergeInfo(withLogoYear(Map.of(
					ZeptoMergeFields.CLINIC_NAME, clinicName == null ? "Clinic" : clinicName,
					"Clinic_Name", clinicName == null ? "Clinic" : clinicName,
					ZeptoMergeFields.ACCEPT_URL, acceptUrl,
					"staff_name", name)));
			mailDto.setRecipientEmail(recipientEmail);
			mailDto.setRecipientName(name);
			dispatch(mailDto, TemplateConstant.ZEPTO_CLINIC_STAFF_INVITE_EMAIL_TEMPLATE_ID);
		} catch (Exception e) {
			log.warn("Failed to send clinic staff invite email to {}: {}", PiiMasker.maskEmail(recipientEmail),
					e.getMessage());
		}
	}

	@Override
	public void sendClinicDoctorInviteReminderEmail(String recipientEmail, String doctorName, String clinicName,
			String acceptUrl) {
		sendDoctorInviteReminder(recipientEmail, doctorName, clinicName, acceptUrl);
	}

	@Override
	public void sendClinicDoctorInviteResponseEmail(String recipientEmail, String clinicName, String doctorName,
			String doctorEmail, boolean accepted) {
		String status = accepted ? "accepted" : "declined";
		log.info("Clinic invite {} — notify clinicEmail={} clinic={} doctor={}", status,
				PiiMasker.maskEmail(recipientEmail), clinicName, doctorName);
		if (recipientEmail == null || recipientEmail.isBlank()) {
			return;
		}
		try {
			ZeptoMailDto mailDto = new ZeptoMailDto();
			String name = clinicName == null || clinicName.isBlank() ? "Clinic" : clinicName;
			
			mailDto.setMergeInfo(withLogoYear(Map.of(
					"doctor_name", doctorName,
					"doctor_email", doctorEmail,
					ZeptoMergeFields.CLINIC_NAME, name,
					"status", status)));
			mailDto.setRecipientEmail(recipientEmail);
			mailDto.setRecipientName(name);
			dispatch(mailDto, TemplateConstant.ZEPTO_CLINIC_DOCTOR_INVITE_RESPONSE_EMAIL_TEMPLATE_ID);
		} catch (Exception e) {
			log.warn("Failed to send clinic invite response email to {}: {}", PiiMasker.maskEmail(recipientEmail),
					e.getMessage());
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
				if (productEntity.getAttributes().getColor() != null && !productEntity.getAttributes().getColor().isEmpty()) {
					product.put("color", Map.of("color", productEntity.getAttributes().getColor()));
				}
				if (productEntity.getAttributes().getSize() != null && !productEntity.getAttributes().getSize().isEmpty()) {
					product.put("size", Map.of("size", productEntity.getAttributes().getSize()));
				}
				if(productEntity.getAttributes().getMaterial() != null && !productEntity.getAttributes().getMaterial().isEmpty()) {
					product.put("material", Map.of("material", productEntity.getAttributes().getMaterial()));
				}
			}

			productsList.add(product);
		}

		// Create the root map with all required fields
		Map<String, Object> root = new HashMap<>();
		root.put("facebook_url", "facebook_url_value");
		root.put("tracking_url", "tracking_url_value");
		root.put("twitter_url", "twitter_url_value");
		root.put("order_number", order.getOrderNumber());
		root.put("tax", order.getTaxes().getOtherTax().add(order.getTaxes().getServiceCharge()).toString());
		root.put("billing_address", order.getBillingAddress().getFormattedAddress());
		root.put("products", productsList); // Pass the list directly
		root.put("total", order.getTotalAmount().toString());
		root.put("shipping", order.getTaxes().getShippingCharges().toString());
		root.put("instagram_url", "instagram_url_value");
		root.put("subtotal", order.getSubTotal().toString());
		root.put("customer_name", user.getFirstName());
		root.put("shipping_address", order.getShippingAddress().getFormattedAddress());
		root.put("logo_url", AppConstant.KITTYP_EMAIL_TEMPLATE_LOGO);
		root.put("current_year", LocalDate.now().getYear());

		// Set merge info directly as a map (no JSON serialization/deserialization)
		mailDto.setMergeInfo(root);
		log.info("ZeptoMail Merge Info: order_number={} productCount={}", order.getOrderNumber(), productsList.size());
		if (!dispatch(mailDto, TemplateConstant.ZEPTO_ORDER_CONFIRMATION_EMAIL_TEMPLATE_ID)) {
			return;
		}
		log.info("Order confirmation email sent to: {}", PiiMasker.maskEmail(recipientEmail));
	}

	@Override
	public void sendPasswordChangedNotification(String email, String customerName, String clinicName, String changeTime) {
		String name = blankToDefault(customerName, "there");
		sendDedicated(email, name, TemplateConstant.ZOHO_ACCOUNT_PASSWORD_CHANGED_TEMPLATE_ID, withLogoYear(Map.of(
				"customer_name", name,
				ZeptoMergeFields.CUSTOMER_NAME, name,
				ZeptoMergeFields.CLINIC_NAME, blankToDefault(clinicName, "KittyP"),
				"change_time", blankToDefault(changeTime, ""),
				"support_url", supportUrl())));
	}

	@Override
	public void sendPhoneChangedNotification(String email, String customerName, String clinicName, String newPhone,
			String loginUrl) {
		String name = blankToDefault(customerName, "there");
		sendDedicated(email, name, TemplateConstant.ZOHO_ACCOUNT_PHONE_CHANGED_TEMPLATE_ID, withLogoYear(Map.of(
				"customer_name", name,
				ZeptoMergeFields.CUSTOMER_NAME, name,
				ZeptoMergeFields.CLINIC_NAME, blankToDefault(clinicName, "KittyP"),
				"new_phone", blankToDefault(newPhone, ""),
				"login_url", blankToDefault(loginUrl, supportUrl()))));
	}

	@Override
	public void sendEmailChangeOtp(String email, String customerName, String clinicName, String otp) {
		String name = blankToDefault(customerName, "there");
		sendDedicated(email, name, TemplateConstant.ZOHO_EMAIL_CHANGE_OTP_TEMPLATE_ID, withLogoYear(Map.of(
				"customer_name", name,
				ZeptoMergeFields.CUSTOMER_NAME, name,
				ZeptoMergeFields.CLINIC_NAME, blankToDefault(clinicName, "KittyP"),
				"otp", otp == null ? "" : otp,
				ZeptoMergeFields.OTP, otp == null ? "" : otp)));
	}

	@Override
	public void sendDoctorInviteReminder(String email, String doctorName, String clinicName, String acceptUrl) {
		log.info("Clinic doctor invite REMINDER to email={} clinic={}", PiiMasker.maskEmail(email), clinicName);
		String name = doctorName == null || doctorName.isBlank() ? "Doctor" : doctorName;
		sendDedicated(email, name, TemplateConstant.ZEPTO_CLINIC_DOCTOR_INVITE_REMINDER_EMAIL_TEMPLATE_ID, withLogoYear(Map.of(
				"doctor_name", name,
				ZeptoMergeFields.CLINIC_NAME, clinicName == null ? "Clinic" : clinicName,
				"accept_url", acceptUrl == null ? "" : acceptUrl,
				ZeptoMergeFields.ACCEPT_URL, acceptUrl == null ? "" : acceptUrl)));
	}

	@Override
	public void sendDoctorProfileVerified(String email, String doctorName, String dashboardUrl) {
		String name = blankToDefault(doctorName, "Doctor");
		sendDedicated(email, name, TemplateConstant.ZOHO_DOCTOR_PROFILE_VERIFIED_TEMPLATE_ID, withLogoYear(Map.of(
				"customer_name", name,
				ZeptoMergeFields.CUSTOMER_NAME, name,
				"doctor_url", blankToDefault(dashboardUrl, ""))));
	}

	@Override
	public void sendClinicProfileVerified(String email, String customerName, String clinicName, String clinicUrl) {
		String name = blankToDefault(customerName, "there");
		sendDedicated(email, name, TemplateConstant.ZOHO_CLINIC_PROFILE_VERIFIED_TEMPLATE_ID, withLogoYear(Map.of(
				"customer_name", name,
				ZeptoMergeFields.CUSTOMER_NAME, name,
				ZeptoMergeFields.CLINIC_NAME, blankToDefault(clinicName, "Clinic"),
				"clinic_url", blankToDefault(clinicUrl, ""))));
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
		mailDto.setMergeInfo(withLogoYear(Map.of(
				"customer_name", name,
				ZeptoMergeFields.CUSTOMER_NAME, name,
				ZeptoMergeFields.CLINIC_NAME, blankToDefault(clinicName, "KittyP Clinic"),
				"pet_name", blankToDefault(petName, "your pet"),
				"invoice_number", blankToDefault(invoiceNumber, ""),
				"amount", blankToDefault(amount, "0.00"),
				"invoice_url", blankToDefault(invoiceUrl, ""))));
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

	private static String blankToDefault(String value, String fallback) {
		return value == null || value.isBlank() ? fallback : value;
	}

	private Map<String, Object> withYear(Map<String, Object> fields) {
		Map<String, Object> merge = new HashMap<>(fields);
		merge.put("current_year", LocalDate.now().getYear());
		return merge;
	}

	private Map<String, Object> withLogoYear(Map<String, Object> fields) {
		Map<String, Object> merge = withYear(fields);
		merge.put("logo_url", AppConstant.KITTYP_EMAIL_TEMPLATE_LOGO);
		return merge;
	}

	/** Agent signup OTP template misspells current_year. Extra current_year blanks the body. */
	private Map<String, Object> withLogoYearTypo(Map<String, Object> fields) {
		Map<String, Object> merge = new HashMap<>(fields);
		merge.put("logo_url", AppConstant.KITTYP_EMAIL_TEMPLATE_LOGO);
		merge.put("currrent_year", LocalDate.now().getYear());
		return merge;
	}

	private String supportUrl() {
		String base = env.getProperty("app.frontend.base-url", "https://kittyp.in");
		if (base == null || base.isBlank()) {
			return "https://kittyp.in";
		}
		return base.endsWith("/") ? base.substring(0, base.length() - 1) : base;
	}

	private void addEmailAuditLog(ZeptoMailResponseModel responseModel, String recipientEmail) {
		EmailAuditDto emailAudit = new EmailAuditDto();
		emailAudit.setRecipientEmail(recipientEmail);
		emailAudit.setMessage(responseModel.getMessage());
		emailAudit.setMessageStatus(responseModel.getData().get(0).getMessage());
		emailAudit.setStatusCode(responseModel.getData().get(0).getCode());
		emailAudit.setRequestId(responseModel.getRequestId());
		emailAudit.setProvider("Zepto Mail");
		emailAudit.setEventName("email_Sent");

		emailAuditService.saveEmailAudit(emailAudit);
		log.info("email audit added for email: {} request id : {}", PiiMasker.maskEmail(recipientEmail),
				responseModel.getRequestId());
	}

}
