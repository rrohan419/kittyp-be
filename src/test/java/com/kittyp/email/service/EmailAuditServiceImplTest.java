package com.kittyp.email.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.env.Environment;
import org.springframework.http.HttpStatus;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.kittyp.common.constants.AppConstant;
import com.kittyp.common.exception.CustomException;
import com.kittyp.common.util.Mapper;
import com.kittyp.email.dao.EmailAuditDao;
import com.kittyp.email.entity.EmailAudit;

@ExtendWith(MockitoExtension.class)
class EmailAuditServiceImplTest {

	private static final String SECRET = "zepto-secret";

	@Mock
	private EmailAuditDao emailAuditDao;
	@Mock
	private Mapper mapper;
	@Mock
	private Environment environment;

	private EmailAuditServiceImpl service;

	@BeforeEach
	void setUp() {
		ObjectMapper objectMapper = new ObjectMapper();
		objectMapper.disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
		service = new EmailAuditServiceImpl(emailAuditDao, mapper, environment, objectMapper);
	}

	@Test
	void deliveredUpdatesTheExistingAudit() {
		stubSecret();
		EmailAudit audit = sentAudit("req-1");
		when(emailAuditDao.emailAuditByRequestId("req-1")).thenReturn(audit);

		service.receiveZeptoWebhook(SECRET, payload("delivered", "req-1", "hook-1", null));

		EmailAudit saved = savedAudit();
		assertEquals("delivered", saved.getEventName());
		assertEquals("hook-1", saved.getWebhookRequestId());
		assertEquals("req-1", saved.getRequestId());
		assertEquals("parent@example.com", saved.getRecipientEmail());
	}

	@Test
	void softbounceStoresTheReason() {
		stubSecret();
		when(emailAuditDao.emailAuditByRequestId("req-1")).thenReturn(sentAudit("req-1"));

		service.receiveZeptoWebhook(SECRET, payload("softbounce", "req-1", "hook-2", "mailbox full"));

		assertEquals("softbounce", savedAudit().getEventName());
		assertEquals("mailbox full", savedAudit().getMessage());
	}

	@Test
	void hardbounceStoresTheDiagnosticMessage() {
		stubSecret();
		when(emailAuditDao.emailAuditByRequestId("req-1")).thenReturn(sentAudit("req-1"));

		service.receiveZeptoWebhook(SECRET,
				payloadWithDiagnostic("hardbounce", "req-1", "hook-3", "user unknown"));

		assertEquals("hardbounce", savedAudit().getEventName());
		assertEquals("user unknown", savedAudit().getMessage());
	}

	@Test
	void sameWebhookRequestIsANoOp() {
		stubSecret();
		EmailAudit audit = sentAudit("req-1");
		audit.setEventName("delivered");
		audit.setWebhookRequestId("hook-1");
		when(emailAuditDao.emailAuditByRequestId("req-1")).thenReturn(audit);

		service.receiveZeptoWebhook(SECRET, payload("delivered", "req-1", "hook-1", null));

		verify(emailAuditDao, never()).save(any());
	}

	@Test
	void repeatedDeliveredEventDoesNotSaveAgain() {
		stubSecret();
		EmailAudit audit = sentAudit("req-1");
		audit.setEventName("delivered");
		audit.setWebhookRequestId("hook-1");
		when(emailAuditDao.emailAuditByRequestId("req-1")).thenReturn(audit);

		service.receiveZeptoWebhook(SECRET, payload("delivered", "req-1", "hook-9", null));

		verify(emailAuditDao, never()).save(any());
	}

	@Test
	void deliveredDoesNotReplaceABounce() {
		stubSecret();
		EmailAudit audit = sentAudit("req-1");
		audit.setEventName("hardbounce");
		audit.setMessage("user unknown");
		audit.setWebhookRequestId("hook-3");
		when(emailAuditDao.emailAuditByRequestId("req-1")).thenReturn(audit);

		service.receiveZeptoWebhook(SECRET, payload("delivered", "req-1", "hook-4", null));

		verify(emailAuditDao, never()).save(any());
		assertEquals("hardbounce", audit.getEventName());
		assertEquals("user unknown", audit.getMessage());
	}

	@Test
	void clientReferenceIsNotTheLookupKey() {
		stubSecret();
		when(emailAuditDao.emailAuditByRequestId("req-1")).thenReturn(sentAudit("req-1"));

		service.receiveZeptoWebhook(SECRET, payload("delivered", "req-1", "hook-1", null)
				.replace("\"subject\": \"Visit\"", "\"subject\": \"Visit\", \"client_reference\": \"KIT-20261006-00001\""));

		verify(emailAuditDao).emailAuditByRequestId("req-1");
		assertEquals("delivered", savedAudit().getEventName());
	}

	@Test
	void unknownRequestIdDoesNotCreateARow() {
		stubSecret();
		when(emailAuditDao.emailAuditByRequestId("missing")).thenReturn(null);

		service.receiveZeptoWebhook(SECRET, payload("delivered", "missing", "hook-1", null));

		verify(emailAuditDao, never()).save(any());
	}

	@Test
	void missingRequestIdDoesNotCreateARow() {
		stubSecret();

		service.receiveZeptoWebhook(SECRET, """
				{"event_name":["delivered"],"event_message":[{"email_info":{"subject":"Visit"}}],"webhook_request_id":"hook-1"}
				""");

		verify(emailAuditDao, never()).emailAuditByRequestId(any());
		verify(emailAuditDao, never()).save(any());
	}

	@Test
	void malformedPayloadIsRejected() {
		stubSecret();

		CustomException ex = assertThrows(CustomException.class,
				() -> service.receiveZeptoWebhook(SECRET, "{not-json"));

		assertEquals(HttpStatus.BAD_REQUEST, ex.getHttpStatus());
		verify(emailAuditDao, never()).save(any());
	}

	@Test
	void emailOpenPayloadDoesNotChangeTheAudit() {
		stubSecret();

		service.receiveZeptoWebhook(SECRET, """
				{"event_name":["email_open"],"event_message":[{"email_info":{"client_reference":"customer-unique-id","email_reference":"2518b.ref","subject":"webhook test email","is_smtp_trigger":false,"is_synced":false,"bounce_address":"bounce@example.com"},"request_id":"req-1"}],"webhook_request_id":"hook-open"}
				""");

		verify(emailAuditDao, never()).emailAuditByRequestId(any());
		verify(emailAuditDao, never()).save(any());
	}

	@Test
	void openClickAndFeedbackDoNotChangeTheAudit() {
		stubSecret();

		service.receiveZeptoWebhook(SECRET, payload("open", "req-1", "hook-1", null));
		service.receiveZeptoWebhook(SECRET, payload("click", "req-1", "hook-2", null));
		service.receiveZeptoWebhook(SECRET, payload("feedback loop", "req-1", "hook-3", null));

		verify(emailAuditDao, never()).emailAuditByRequestId(any());
		verify(emailAuditDao, never()).save(any());
	}

	@Test
	void invalidSecretDoesNotChangeTheAudit() {
		when(environment.getProperty(AppConstant.ZEPTOMAIL_WEBHOOK_SECRET)).thenReturn(SECRET);

		CustomException ex = assertThrows(CustomException.class,
				() -> service.receiveZeptoWebhook("wrong-secret", payload("delivered", "req-1", "hook-1", null)));

		assertEquals(HttpStatus.UNAUTHORIZED, ex.getHttpStatus());
		assertEquals("Invalid webhook secret", ex.getMessage());
		verify(emailAuditDao, never()).save(any());
	}

	@Test
	void missingSecretDoesNotChangeTheAudit() {
		when(environment.getProperty(AppConstant.ZEPTOMAIL_WEBHOOK_SECRET)).thenReturn(SECRET);

		CustomException ex = assertThrows(CustomException.class,
				() -> service.receiveZeptoWebhook("  ", payload("delivered", "req-1", "hook-1", null)));

		assertEquals(HttpStatus.UNAUTHORIZED, ex.getHttpStatus());
		assertEquals("Missing webhook secret", ex.getMessage());
	}

	private void stubSecret() {
		when(environment.getProperty(AppConstant.ZEPTOMAIL_WEBHOOK_SECRET)).thenReturn(SECRET);
	}

	private EmailAudit savedAudit() {
		ArgumentCaptor<EmailAudit> captor = ArgumentCaptor.forClass(EmailAudit.class);
		verify(emailAuditDao).save(captor.capture());
		return captor.getValue();
	}

	private static EmailAudit sentAudit(String requestId) {
		EmailAudit audit = new EmailAudit();
		audit.setRequestId(requestId);
		audit.setEventName("email_Sent");
		audit.setRecipientEmail("parent@example.com");
		audit.setMessage("sent");
		return audit;
	}

	private static String payload(String event, String requestId, String webhookRequestId, String reason) {
		String reasonJson = reason == null ? "" : ",\"reason\":\"" + reason + "\"";
		return """
				{"event_name":["%s"],"event_message":[{"request_id":"%s","email_info":{"email_reference":"mail-ref","subject":"Visit","to":[{"email_address":{"address":"other@example.com"}}]},"event_data":[{"details":[{"time":"2026-10-06T10:00:00Z"%s}]}]}],"webhook_request_id":"%s"}
				""".formatted(event, requestId, reasonJson, webhookRequestId);
	}

	private static String payloadWithDiagnostic(String event, String requestId, String webhookRequestId,
			String diagnostic) {
		return """
				{"event_name":["%s"],"event_message":[{"request_id":"%s","email_info":{"email_reference":"mail-ref"},"event_data":[{"details":[{"diagnostic_message":"%s"}]}]}],"webhook_request_id":"%s"}
				""".formatted(event, requestId, diagnostic, webhookRequestId);
	}
}
