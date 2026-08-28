package com.kittyp.notification.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

import com.kittyp.common.exception.CustomException;

class WhatsAppPhonesTest {

    @Test
    void tenDigitIndianNumberGetsCountryCode() {
        assertEquals("919876543210", WhatsAppPhones.toE164Digits("9876543210", "91"));
        assertEquals("919876543210", WhatsAppPhones.toE164Digits("98765-43210", "91"));
        assertEquals("919876543210", WhatsAppPhones.toE164Digits("+91 98765 43210", "91"));
    }

    @Test
    void trunkZeroIsStripped() {
        assertEquals("919876543210", WhatsAppPhones.toE164Digits("09876543210", "91"));
    }

    @Test
    void international00Prefix() {
        assertEquals("919876543210", WhatsAppPhones.toE164Digits("00919876543210", "91"));
    }

    @Test
    void rejectsBlankAndTooShort() {
        assertThrows(CustomException.class, () -> WhatsAppPhones.toE164Digits(" ", "91"));
        assertThrows(CustomException.class, () -> WhatsAppPhones.toE164Digits("12345", "91"));
    }

    @Test
    void sanitizeTemplateText() {
        assertEquals("-", WhatsAppPhones.sanitizeTemplateText(null));
        assertEquals("Line A Line B", WhatsAppPhones.sanitizeTemplateText("Line A\nLine B"));
    }

    @Test
    void redactKeepsLastFour() {
        assertEquals("***3210", WhatsAppPhones.redact("919876543210"));
        assertEquals("***", WhatsAppPhones.redact(""));
        assertEquals("***", WhatsAppPhones.redact(null));
    }
}
