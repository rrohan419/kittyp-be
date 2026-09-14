package com.kittyp.notification.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.stereotype.Service;

/**
 * Fallback SMS implementation: logs the OTP when no real gateway bean exists.
 * SmsGatewayService (TextBee) or TwilioSmsService replace this when configured.
 */
@Service
@ConditionalOnMissingBean(SmsService.class)
public class LoggingSmsService implements SmsService {

	private static final Logger log = LoggerFactory.getLogger(LoggingSmsService.class);

	@Override
	public void sendOtp(String phoneNumber, String code) {
		log.info("SMS OTP to phone={} code={} (logging SMS — configure Twilio for real delivery)", phoneNumber, code);
	}
}
