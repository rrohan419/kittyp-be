package com.kittyp.notification.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.test.util.ReflectionTestUtils;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.kittyp.common.exception.CustomException;
import com.kittyp.notification.repository.NotificationLogRepository;

@ExtendWith(MockitoExtension.class)
class OutboundMessageServiceTest {

    @Mock
    private WhatsAppService whatsAppService;
    @Mock
    private NotificationLogRepository notificationLogRepository;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @InjectMocks
    private OutboundMessageService outboundMessageService;

    private final WhatsAppSenderCredentials sender =
            WhatsAppSenderCredentials.of("tok", "phone-1", WhatsAppConnectionStatuses.TEMPLATE_APPROVED);

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(outboundMessageService, "invoiceTemplate", "invoice_receipt");
        ReflectionTestUtils.setField(outboundMessageService, "invoiceTemplateLang", "en");
        ReflectionTestUtils.setField(outboundMessageService, "vaccineTemplate", "vaccine_reminder");
        ReflectionTestUtils.setField(outboundMessageService, "checkupTemplate", "checkup_reminder");
        ReflectionTestUtils.setField(outboundMessageService, "promoTemplate", "promo_offer");
        ReflectionTestUtils.setField(outboundMessageService, "defaultLang", "en");
        ReflectionTestUtils.setField(outboundMessageService, "objectMapper", objectMapper);
    }

    @Test
    void requireSenderReadyFailsWhenCredentialsMissing() {
        CustomException ex = assertThrows(CustomException.class,
                () -> outboundMessageService.requireSenderReady(
                        WhatsAppSenderCredentials.of(null, null), "doctor"));
        assertEquals(HttpStatus.SERVICE_UNAVAILABLE, ex.getHttpStatus());
        assertTrue(ex.getMessage().contains("Add Meta Phone Number ID"));
    }

    @Test
    void requireSenderReadyFailsWhenDeliveryDisabled() {
        when(whatsAppService.isDeliveryEnabled()).thenReturn(false);
        CustomException ex = assertThrows(CustomException.class,
                () -> outboundMessageService.requireSenderReady(sender, "clinic"));
        assertEquals(HttpStatus.SERVICE_UNAVAILABLE, ex.getHttpStatus());
        assertTrue(ex.getMessage().contains("WHATSAPP_ENABLED"));
    }

    @Test
    void requireSenderReadyPassesWhenCredentialsAndDeliveryOk() {
        when(whatsAppService.isDeliveryEnabled()).thenReturn(true);
        outboundMessageService.requireSenderReady(sender, "clinic");
    }

    @Test
    void sendInvoiceBlockedWhenTemplateNotApproved() {
        WhatsAppSenderCredentials pending =
                WhatsAppSenderCredentials.of("tok", "phone-1", WhatsAppConnectionStatuses.TEMPLATE_PENDING);
        CustomException ex = assertThrows(CustomException.class,
                () -> outboundMessageService.sendInvoicePdfWhatsApp(
                        pending, "9876543210", new byte[] {1}, "a.pdf", List.of("a"), null, null));
        assertEquals(HttpStatus.SERVICE_UNAVAILABLE, ex.getHttpStatus());
        assertTrue(ex.getMessage().toLowerCase().contains("not approved"));
        verify(whatsAppService, never()).uploadDocumentPdf(any(), any(), anyString());
    }

    @Test
    void sendInvoicePdfUploadsThenTemplates() {
        when(whatsAppService.toE164Digits("9876543210")).thenReturn("919876543210");
        when(whatsAppService.uploadDocumentPdf(eq(sender), any(), eq("INV.pdf"))).thenReturn("media-1");

        outboundMessageService.sendInvoicePdfWhatsApp(
                sender,
                "9876543210",
                new byte[] {1, 2, 3},
                "INV.pdf",
                List.of("Owner", "Clinic", "Pet", "INV-1", "100.00"),
                null,
                null);

        verify(whatsAppService).uploadDocumentPdf(eq(sender), any(), eq("INV.pdf"));
        verify(whatsAppService).sendDocumentTemplate(
                eq(sender),
                eq("919876543210"),
                eq("invoice_receipt"),
                eq("en"),
                eq("media-1"),
                eq("INV.pdf"),
                anyList());
        verify(notificationLogRepository, never()).save(any());
    }

    @Test
    void sendVaccineReminderUsesTextTemplate() {
        when(whatsAppService.toE164Digits(anyString())).thenReturn("919876543210");

        outboundMessageService.sendVaccineReminder(
                sender, "9876543210", List.of("Buddy", "Rabies", "2026-09-01"), null, null);

        verify(whatsAppService).sendTextTemplate(
                eq(sender), eq("919876543210"), eq("vaccine_reminder"), eq("en"), anyList());
    }

    @Test
    void sendInvoicePropagatesUploadFailure() {
        when(whatsAppService.toE164Digits(anyString())).thenReturn("919876543210");
        doThrow(new CustomException("upload failed", HttpStatus.BAD_GATEWAY))
                .when(whatsAppService).uploadDocumentPdf(eq(sender), any(), anyString());

        assertThrows(CustomException.class, () -> outboundMessageService.sendInvoicePdfWhatsApp(
                sender, "9876543210", new byte[] {1}, "a.pdf", List.of("a"), null, null));
        verify(whatsAppService, never()).sendDocumentTemplate(
                any(), anyString(), anyString(), anyString(), anyString(), anyString(), anyList());
    }
}
