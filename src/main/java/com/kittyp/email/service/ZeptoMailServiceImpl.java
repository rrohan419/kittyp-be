/**
 * @author rrohan419@gmail.com
 */
package com.kittyp.email.service;

import java.util.Map;

import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import com.kittyp.common.constants.TemplateConstant;
import com.kittyp.common.util.VerificationCodeService;
import com.kittyp.email.dto.EmailAuditDto;
import com.kittyp.email.dto.ZeptoMailDto;
import com.kittyp.email.emailsender.ZeptoMailSender;
import com.kittyp.email.model.ZeptoMailResponseModel;
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

	/**
	 * @author rrohan419@gmail.com
	 */
	@Async
	@Override
	public void sendWelcomeEmail(String recipientEmail) {
		User user = userDao.userByEmail(recipientEmail);
		
		ZeptoMailDto mailDto = new ZeptoMailDto();
		mailDto.setMergeInfo(Map.of("Customer_Name", user.getFirstName()));
		mailDto.setRecipientEmail(recipientEmail);
		mailDto.setRecipientName(user.getFirstName());
		mailDto.setTemplateKey(TemplateConstant.ZEPTO_WELCOME_EMAIL_TEMPLATE_ID);
//		mailDto.setProvider("Zepto Mail");

		ZeptoMailResponseModel responseModel = zeptoMailSender.sendEmail(mailDto);
		log.info("welcome email sent for email : "+recipientEmail);
		addEmailAuditLog(responseModel, recipientEmail);

	}

	/**
	 * @author rrohan419@gmail.com
	 */
	@Override
	public void sendPasswordResetCode(String email) {
		User user = userDao.userByEmail(email);

		ZeptoMailDto mailDto = new ZeptoMailDto();
		mailDto.setMergeInfo(Map.of("Customer_Name", user.getFirstName(), "RESET_CODE", verificationCodeService.generateCode(user.getUuid())));
		mailDto.setRecipientEmail(email);
		mailDto.setRecipientName(user.getFirstName());
		mailDto.setTemplateKey(TemplateConstant.ZEPTO_RESET_PASSWORD_CODE_EMAIL_TEMPLATE_ID);

		ZeptoMailResponseModel responseModel = zeptoMailSender.sendEmail(mailDto);
		log.info("password reset code sent for email : "+email);
		addEmailAuditLog(responseModel, email);

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
		log.info("email audit added for email: "+recipientEmail+" request id : "+responseModel.getRequestId());
	}

}
