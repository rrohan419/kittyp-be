package com.kittyp.common.logging;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import org.junit.jupiter.api.Test;

import ch.qos.logback.classic.spi.ILoggingEvent;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class PiiMaskerTest {

	@Test
	void maskPhone_keepsCountryFirstTwoAndLastTwo() {
		assertEquals("+9198******10", PiiMasker.maskPhone("+919876543210"));
		assertEquals("+9198******10", PiiMasker.maskPhone("9876543210"));
	}

	@Test
	void maskEmail_keepsFirstLetter() {
		assertEquals("j***@domain.com", PiiMasker.maskEmail("jane@domain.com"));
	}

	@Test
	void mask_redactsSecretFieldsAndEmails() {
		String masked = PiiMasker.mask("otp=654321 token=abc email=jane@domain.com +919876543210");
		assertFalse(masked.contains("654321"));
		assertFalse(masked.contains("abc"));
		assertEquals(true, masked.contains("j***@domain.com"));
		assertEquals(true, masked.contains("+9198******10"));
	}

	@Test
	void converter_masksFormattedMessage() {
		ILoggingEvent event = mock(ILoggingEvent.class);
		when(event.getFormattedMessage()).thenReturn("otp=654321 user=jane@domain.com");
		assertEquals("otp=*** user=j***@domain.com", new PiiMaskingConverter().convert(event));
	}
}
