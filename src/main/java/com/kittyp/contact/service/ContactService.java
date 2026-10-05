package com.kittyp.contact.service;

import java.util.Map;
import java.util.UUID;

import org.springframework.core.env.Environment;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import com.kittyp.common.constants.AppConstant;
import com.kittyp.common.constants.TemplateConstant;
import com.kittyp.common.exception.CustomException;
import com.kittyp.contact.dto.ContactRequest;
import com.kittyp.email.dto.ZeptoMailDto;
import com.kittyp.email.emailsender.ZeptoMailSender;
import com.kittyp.support.service.InboundMail;
import com.kittyp.support.service.SupportMailService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class ContactService {

	private final ZeptoMailSender zeptoMailSender;
	private final Environment env;
	private final SupportMailService supportMailService;

	public void send(ContactRequest request) {
		String from = env.getProperty(AppConstant.KITTYP_MAIL_ID);
		String support = env.getProperty(AppConstant.KITTYP_SUPPORT_MAIL_ID, "admin@kittyp.in");
		String templateKey = env.getProperty(TemplateConstant.ZOHO_CONTACT_ADMIN_EMAIL_TEMPLATE_ID);
		if (from == null || from.isBlank() || support == null || support.isBlank()
				|| templateKey == null || templateKey.isBlank()) {
			throw new CustomException("Mail is not configured", HttpStatus.SERVICE_UNAVAILABLE);
		}
		String name = request.getName().trim();
		String email = request.getEmail().trim();
		String subject = request.getSubject().trim();
		String message = request.getMessage().trim();
		String supportId = supportMailService.receive(
				new InboundMail("contact-" + UUID.randomUUID(), email, subject, message, null, name));
		if (supportId == null || supportId.isBlank()) {
			throw new CustomException("Could not send your message. Please email admin@kittyp.in.",
					HttpStatus.BAD_GATEWAY);
		}
		ZeptoMailDto mail = new ZeptoMailDto();
		mail.setTemplateKey(templateKey.trim());
		mail.setRecipientEmail(support.trim());
		mail.setRecipientName("KittyP Support");
		mail.setReplyToEmail(email);
		mail.setReplyToName(name);
		mail.setMergeInfo(Map.of(
				"Customer_Name", name,
				"Customer_Email", email,
				"Subject", subject,
				"Message", message,
				"Support_Id", supportId));
		try {
			zeptoMailSender.sendEmail(mail);
		} catch (RuntimeException e) {
			log.warn("Contact form mail failed: {}", e.getMessage());
			throw new CustomException("Could not send your message. Please email admin@kittyp.in.",
					HttpStatus.BAD_GATEWAY);
		}
	}
}
