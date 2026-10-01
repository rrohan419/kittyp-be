package com.kittyp.contact.service;

import java.util.UUID;

import org.springframework.core.env.Environment;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import com.kittyp.common.constants.AppConstant;
import com.kittyp.common.exception.CustomException;
import com.kittyp.contact.ContactMail;
import com.kittyp.contact.dto.ContactRequest;
import com.kittyp.email.dto.ZohoMailRequest;
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
		if (from == null || from.isBlank() || support == null || support.isBlank()) {
			throw new CustomException("Mail is not configured", HttpStatus.SERVICE_UNAVAILABLE);
		}
		ZohoMailRequest mail = ContactMail.build(from, support, request);
		try {
			zeptoMailSender.sendHtml(mail);
		} catch (RuntimeException e) {
			log.warn("Contact form mail failed: {}", e.getMessage());
			throw new CustomException("Could not send your message. Please email admin@kittyp.in.",
					HttpStatus.BAD_GATEWAY);
		}
		supportMailService.receive(new InboundMail("contact-" + UUID.randomUUID(), request.getEmail().trim(),
				request.getSubject().trim(), request.getMessage().trim(), null));
	}
}
