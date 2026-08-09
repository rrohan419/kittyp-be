package com.kittyp.notification.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.kittyp.common.exception.CustomException;

/**
 * Ensures Cloud API never treats empty entity credentials as "configured"
 * (no silent platform / env fallback for tenant sends).
 */
class WhatsAppCloudApiServiceNoFallbackTest {

    private WhatsAppCloudApiService service;

    @BeforeEach
    void setUp() {
        service = new WhatsAppCloudApiService(new ObjectMapper(), "v21.0", "91");
    }

    @Test
    void isConfiguredFalseWhenSenderMissing() {
        assertFalse(service.isConfigured(null));
        assertFalse(service.isConfigured(WhatsAppSenderCredentials.of("", "")));
        assertFalse(service.isConfigured(WhatsAppSenderCredentials.of("tok", "")));
    }

    @Test
    void uploadRejectsUnconfiguredSender() {
        CustomException ex = assertThrows(CustomException.class,
                () -> service.uploadDocumentPdf(WhatsAppSenderCredentials.of(null, null), new byte[] {1}, "a.pdf"));
        assertEquals(HttpStatus.SERVICE_UNAVAILABLE, ex.getHttpStatus());
    }

    @Test
    void sendTemplateRejectsUnconfiguredSender() {
        CustomException ex = assertThrows(CustomException.class,
                () -> service.sendDocumentTemplate(
                        WhatsAppSenderCredentials.of("tok", ""),
                        "919876543210",
                        "invoice_receipt",
                        "en",
                        "media",
                        "a.pdf",
                        java.util.List.of("a")));
        assertEquals(HttpStatus.SERVICE_UNAVAILABLE, ex.getHttpStatus());
    }
}
