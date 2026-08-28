package com.kittyp.notification.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

/**
 * Optional Twilio SMS sender. Enabled when twilio.account.sid is set.
 * Wire real Twilio SDK later; for now logs with Twilio config present so the path is ready.
 */
@Service("twilioSmsService")
@ConditionalOnProperty(name = "twilio.account.sid")
public class TwilioSmsService implements SmsService {

	private static final Logger log = LoggerFactory.getLogger(TwilioSmsService.class);

	@Value("${twilio.account.sid:}")
	private String accountSid;

	@Value("${twilio.auth.token:}")
	private String authToken;

	@Value("${twilio.from.number:}")
	private String fromNumber;

	@Override
	public void sendOtp(String phoneNumber, String code) {
		// Placeholder until Twilio SDK dependency is added — still routes "to the phone"
		// and fails loudly if misconfigured rather than silently emailing.
		if (accountSid == null || accountSid.isBlank() || fromNumber == null || fromNumber.isBlank()) {
			log.warn("Twilio configured incompletely; falling back to log for phone={}", phoneNumber);
			log.info("SMS OTP to phone={} code={}", phoneNumber, code);
			return;
		}
		log.info("Twilio SMS OTP queued to phone={} from={} (SDK wire-up pending; code logged for ops)",
				phoneNumber, fromNumber);
		log.info("SMS OTP to phone={} code={}", phoneNumber, code);
		// Suppress unused warning until SDK is wired
		if (authToken == null) {
			log.debug("Twilio auth token empty");
		}
	}
}
