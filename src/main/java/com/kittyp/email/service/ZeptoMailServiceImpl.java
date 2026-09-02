/**
 * @author rrohan419@gmail.com
 */
package com.kittyp.email.service;

import java.util.ArrayList;
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
import com.kittyp.common.util.VerificationCodeService;
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
		mailDto.setMergeInfo(
				Map.of("Customer_Name",firstName));
		mailDto.setRecipientEmail(recipientEmail);
		mailDto.setRecipientName(firstName);
		mailDto.setTemplateKey(env.getProperty(TemplateConstant.ZOHO_PARENT_WELCOME_EMAIL_TEMPLATE_ID));
		// mailDto.setProvider("Zepto Mail");

		ZeptoMailResponseModel responseModel = zeptoMailSender.sendEmail(mailDto);
		log.info("welcome email sent for email : " + recipientEmail);
		addEmailAuditLog(responseModel, recipientEmail);

	}

	@Override
	public void sendWelcomeEmailforDoctor(String recipientEmail) {
		User user = userDao.userByEmail(recipientEmail);
		ZeptoMailDto mailDto = new ZeptoMailDto();
		mailDto.setMergeInfo(Map.of("Customer_Name", user.getFirstName(), "logo_url", AppConstant.KITTYP_EMAIL_TEMPLATE_LOGO));
		mailDto.setRecipientEmail(recipientEmail);
		mailDto.setRecipientName(user.getFirstName());
		mailDto.setTemplateKey(env.getProperty(TemplateConstant.ZOHO_DOCTOR_WELCOME_EMAIL_TEMPLATE_ID));
		
		ZeptoMailResponseModel responseModel = zeptoMailSender.sendEmail(mailDto);
		log.info("welcome email sent for email : " + recipientEmail);
		addEmailAuditLog(responseModel, recipientEmail);
	}

	@Override
	public void sendWelcomeEmailforClinicAdmin(String recipientEmail) {
		User user = userDao.userByEmail(recipientEmail);
		ZeptoMailDto mailDto = new ZeptoMailDto();
		mailDto.setMergeInfo(Map.of("Customer_Name", user.getFirstName()));
		mailDto.setRecipientEmail(recipientEmail);
		mailDto.setRecipientName(user.getFirstName());
		mailDto.setTemplateKey(env.getProperty(TemplateConstant.ZOHO_CLINIC_ADMIN_WELCOME_EMAIL_TEMPLATE_ID));
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
		mailDto.setMergeInfo(Map.of("Customer_Name", user.getFirstName(), "RESET_CODE",
				code, "logo_url",
				AppConstant.KITTYP_EMAIL_TEMPLATE_LOGO));
		mailDto.setRecipientEmail(email);
		mailDto.setRecipientName(user.getFirstName());
		mailDto.setTemplateKey(TemplateConstant.ZEPTO_RESET_PASSWORD_CODE_EMAIL_TEMPLATE_ID);

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
					: "Doctor Applicant";
			mailDto.setMergeInfo(Map.of(
					"Customer_Name", name,
					"RESET_CODE", code,
					"logo_url", AppConstant.KITTYP_EMAIL_TEMPLATE_LOGO));
			mailDto.setRecipientEmail(recipientEmail);
			mailDto.setRecipientName(name);
			mailDto.setTemplateKey(TemplateConstant.ZEPTO_RESET_PASSWORD_CODE_EMAIL_TEMPLATE_ID);
			ZeptoMailResponseModel responseModel = zeptoMailSender.sendEmail(mailDto);
			addEmailAuditLog(responseModel, recipientEmail);
		} catch (Exception e) {
			// OTP is still in cache / logs — don't fail signup OTP in local if mail provider is down
			log.warn("Failed to send signup OTP email to {}: {}", recipientEmail, e.getMessage());
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
					"Customer_Name", name,
					"RESET_CODE", acceptUrl,
					"logo_url", AppConstant.KITTYP_EMAIL_TEMPLATE_LOGO));
			mailDto.setRecipientEmail(recipientEmail);
			mailDto.setRecipientName(name);
			mailDto.setTemplateKey(TemplateConstant.ZEPTO_RESET_PASSWORD_CODE_EMAIL_TEMPLATE_ID);
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
					"Customer_Name", name,
					"RESET_CODE", acceptUrl,
					"logo_url", AppConstant.KITTYP_EMAIL_TEMPLATE_LOGO));
			mailDto.setRecipientEmail(recipientEmail);
			mailDto.setRecipientName(name);
			mailDto.setTemplateKey(TemplateConstant.ZEPTO_RESET_PASSWORD_CODE_EMAIL_TEMPLATE_ID);
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
		sendClinicDoctorInviteEmail(recipientEmail, doctorName, clinicName, acceptUrl);
	}

	@Override
	public void sendClinicDoctorInviteResponseEmail(String recipientEmail, String clinicName, String doctorName,
			String doctorEmail, boolean accepted) {
		String action = accepted ? "accepted" : "declined";
		log.info("Clinic invite {} — notify clinicEmail={} clinic={} doctor={} <{}>", action, recipientEmail,
				clinicName, doctorName, doctorEmail);
		if (recipientEmail == null || recipientEmail.isBlank()) {
			return;
		}
		try {
			ZeptoMailDto mailDto = new ZeptoMailDto();
			String name = clinicName == null || clinicName.isBlank() ? "Clinic" : clinicName;
			String body = String.format("%s (%s) %s your invite to join %s.",
					doctorName == null || doctorName.isBlank() ? "A doctor" : doctorName,
					doctorEmail == null ? "" : doctorEmail, action, name);
			mailDto.setMergeInfo(Map.of(
					"Customer_Name", name,
					"RESET_CODE", body,
					"logo_url", AppConstant.KITTYP_EMAIL_TEMPLATE_LOGO));
			mailDto.setRecipientEmail(recipientEmail);
			mailDto.setRecipientName(name);
			mailDto.setTemplateKey(TemplateConstant.ZEPTO_RESET_PASSWORD_CODE_EMAIL_TEMPLATE_ID);
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
		mailDto.setTemplateKey(TemplateConstant.ZEPTO_ORDER_CONFIRMATION_EMAIL_TEMPLATE_ID);

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
		root.put("logo_url", "logo_url_value");
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
		root.put("tracking_url", "tracking_url_value");
		root.put("twitter_url", "twitter_url_value");
		root.put("logo_url", AppConstant.KITTYP_EMAIL_TEMPLATE_LOGO);

		// Set merge info directly as a map (no JSON serialization/deserialization)
		mailDto.setMergeInfo(root);
		log.info("ZeptoMail Merge Info: {}", root);

		ZeptoMailResponseModel responseModel = zeptoMailSender.sendEmail(mailDto);
		log.info("Order confirmation email sent to: {}", recipientEmail);
		addEmailAuditLog(responseModel, recipientEmail);
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
		log.info("email audit added for email: " + recipientEmail + " request id : " + responseModel.getRequestId());
	}

}
