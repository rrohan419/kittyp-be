/**
 * @author rrohan419@gmail.com
 */
package com.kittyp.email.service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

import org.springframework.core.env.Environment;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.kittyp.common.constants.AppConstant;
import com.kittyp.common.exception.CustomException;
import com.kittyp.common.util.Mapper;
import com.kittyp.email.dao.EmailAuditDao;
import com.kittyp.email.dto.EmailAuditDto;
import com.kittyp.email.entity.EmailAudit;
import com.kittyp.email.model.Detail;
import com.kittyp.email.model.EventData;
import com.kittyp.email.model.EventMessage;
import com.kittyp.email.model.ZeptoWebhookEventRequest;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * @author rrohan419@gmail.com
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class EmailAuditServiceImpl implements EmailAuditService {

	private static final int MESSAGE_MAX = 255;

	private final EmailAuditDao emailAuditDao;
	private final Mapper mapper;
	private final Environment environment;
	private final ObjectMapper objectMapper;

	/**
	 * @author rrohan419@gmail.com
	 */
	@Override
	public void saveEmailAudit(EmailAuditDto emailAuditDto) {
		EmailAudit emailAudit = mapper.convert(emailAuditDto, EmailAudit.class);
		emailAuditDao.save(emailAudit);
	}

	/**
	 * Authenticates the CPaaS call, then records delivered, softbounce, or hardbounce on the existing audit row.
	 */
	@Override
	public void receiveZeptoWebhook(String presentedSecret, String rawBody) {
		verifySecret(presentedSecret);
		if (rawBody == null || rawBody.isBlank()) {
			throw new CustomException("Invalid webhook payload", HttpStatus.BAD_REQUEST);
		}
		ZeptoWebhookEventRequest request;
		try {
			request = objectMapper.readValue(rawBody, ZeptoWebhookEventRequest.class);
		} catch (JsonProcessingException ex) {
			throw new CustomException("Invalid webhook payload", HttpStatus.BAD_REQUEST);
		}
		zeptoWebhookEmailAudit(request);
	}

	/**
	 * Updates the audit created at send time. Open, click, and other events are accepted and left unchanged.
	 */
	private void zeptoWebhookEmailAudit(ZeptoWebhookEventRequest webhookEventRequest) {
		String eventName = eventName(webhookEventRequest);
		if (!isDeliveryEvent(eventName)) {
			log.info("ZeptoMail webhook accepted without audit change: {}", eventName);
			return;
		}
		if (webhookEventRequest.getEventMessage() == null || webhookEventRequest.getEventMessage().isEmpty()) {
			log.info("ZeptoMail webhook {} has no event message", eventName);
			return;
		}
		for (EventMessage eventMessage : webhookEventRequest.getEventMessage()) {
			applyDeliveryEvent(eventName, webhookEventRequest.getWebhookRequestId(), eventMessage);
		}
	}

	private void applyDeliveryEvent(String eventName, String webhookRequestId, EventMessage eventMessage) {
		if (eventMessage == null || eventMessage.getRequestId() == null || eventMessage.getRequestId().isBlank()) {
			log.info("ZeptoMail webhook {} has no request id", eventName);
			return;
		}
		EmailAudit emailAudit = emailAuditDao.emailAuditByRequestId(eventMessage.getRequestId());
		if (emailAudit == null) {
			log.info("ZeptoMail webhook {} matched no email audit", eventName);
			return;
		}
		if (webhookRequestId != null && webhookRequestId.equals(emailAudit.getWebhookRequestId())) {
			return;
		}
		if (rank(emailAudit.getEventName()) >= rank(eventName)) {
			return;
		}
		emailAudit.setEventName(eventName);
		emailAudit.setWebhookRequestId(webhookRequestId);
		String reason = bounceReason(eventMessage);
		if (reason != null) {
			emailAudit.setMessage(reason);
		}
		emailAuditDao.save(emailAudit);
	}

	private void verifySecret(String presented) {
		String expected = environment.getProperty(AppConstant.ZEPTOMAIL_WEBHOOK_SECRET);
		if (expected == null || expected.isBlank()) {
			log.info("ZeptoMail webhook secret is not configured");
			throw new CustomException("Missing webhook secret", HttpStatus.UNAUTHORIZED);
		}
		if (presented == null || presented.isBlank()) {
			throw new CustomException("Missing webhook secret", HttpStatus.UNAUTHORIZED);
		}
		byte[] expectedBytes = expected.getBytes(StandardCharsets.UTF_8);
		byte[] presentedBytes = presented.getBytes(StandardCharsets.UTF_8);
		if (!MessageDigest.isEqual(expectedBytes, presentedBytes)) {
			throw new CustomException("Invalid webhook secret", HttpStatus.UNAUTHORIZED);
		}
	}

	private static String eventName(ZeptoWebhookEventRequest request) {
		if (request == null || request.getEventName() == null || request.getEventName().isEmpty()
				|| request.getEventName().get(0) == null || request.getEventName().get(0).isBlank()) {
			throw new CustomException("Invalid webhook payload", HttpStatus.BAD_REQUEST);
		}
		return normalize(request.getEventName().get(0));
	}

	private static boolean isDeliveryEvent(String eventName) {
		return "delivered".equals(eventName) || "softbounce".equals(eventName) || "hardbounce".equals(eventName);
	}

	/** Higher rank must not be replaced by a later, weaker delivery status. */
	private static int rank(String eventName) {
		String normalized = normalize(eventName);
		if ("hardbounce".equals(normalized)) {
			return 3;
		}
		if ("softbounce".equals(normalized)) {
			return 2;
		}
		if ("delivered".equals(normalized)) {
			return 1;
		}
		return 0;
	}

	private static String normalize(String eventName) {
		if (eventName == null) {
			return "";
		}
		return eventName.trim().toLowerCase().replace("_", "").replace(" ", "");
	}

	private static String bounceReason(EventMessage eventMessage) {
		if (eventMessage.getEventData() == null) {
			return null;
		}
		for (EventData eventData : eventMessage.getEventData()) {
			if (eventData == null || eventData.getDetails() == null) {
				continue;
			}
			for (Detail detail : eventData.getDetails()) {
				if (detail == null) {
					continue;
				}
				String reason = firstText(detail.getReason(), detail.getDiagnosticMessage());
				if (reason != null) {
					return reason.length() <= MESSAGE_MAX ? reason : reason.substring(0, MESSAGE_MAX);
				}
			}
		}
		return null;
	}

	private static String firstText(String primary, String fallback) {
		if (primary != null && !primary.isBlank()) {
			return primary.trim();
		}
		if (fallback != null && !fallback.isBlank()) {
			return fallback.trim();
		}
		return null;
	}
}
