package com.kittyp.notification.service;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class WhatsAppSenderCredentialsTest {

    @Test
    void requiresBothTokenAndPhoneNumberId() {
        assertFalse(WhatsAppSenderCredentials.of(null, null).isConfigured());
        assertFalse(WhatsAppSenderCredentials.of("tok", null).isConfigured());
        assertFalse(WhatsAppSenderCredentials.of(null, "123").isConfigured());
        assertFalse(WhatsAppSenderCredentials.of("  ", "123").isConfigured());
        assertFalse(WhatsAppSenderCredentials.of("tok", "  ").isConfigured());
        assertTrue(WhatsAppSenderCredentials.of("tok", "123").isConfigured());
    }

    @Test
    void trimsWhitespace() {
        WhatsAppSenderCredentials c = WhatsAppSenderCredentials.of("  tok  ", " 999 ");
        assertTrue(c.isConfigured());
        assertTrue(c.token().equals("tok"));
        assertTrue(c.phoneNumberId().equals("999"));
    }
}
