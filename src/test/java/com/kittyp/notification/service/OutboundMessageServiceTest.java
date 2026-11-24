package com.kittyp.notification.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.doThrow;
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
    private KittypWhatsAppMessagingService kittypWhatsAppMessaging;
    @Mock
    private NotificationLogRepository notificationLogRepository;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @InjectMocks
    private OutboundMessageService outboundMessageService;

    private final WhatsAppSenderCredentials sender =
            WhatsAppSenderCredentials.of("kittyp-token", "kittyp-phone-id");

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(outboundMessageService, "invoiceTemplate", " invoice ");
        ReflectionTestUtils.setField(outboundMessageService, "invoiceTemplateLang", " en ");
        ReflectionTestUtils.setField(outboundMessageService, "vaccineTemplate", "vaccine_reminder");
        ReflectionTestUtils.setField(outboundMessageService, "checkupTemplate", "checkup_reminder");
        ReflectionTestUtils.setField(outboundMessageService, "promoTemplate", "promo_offer");
        ReflectionTestUtils.setField(outboundMessageService, "defaultLang", "en");
        ReflectionTestUtils.setField(outboundMessageService, "objectMapper", objectMapper);
    }

    private void stubSenderReady() {
        when(kittypWhatsAppMessaging.requireSender()).thenReturn(sender);
        when(whatsAppService.isDeliveryEnabled()).thenReturn(true);
        when(whatsAppService.isConfigured(sender)).thenReturn(true);
    }

    private void stubInvoiceTemplate() {
        when(kittypWhatsAppMessaging.requireInvoiceDocumentTemplate("invoice", "en"))
                .thenReturn(new com.kittyp.notification.model.MessagingDtos.ApprovedTemplate(
                        "invoice__en", "invoice", "WHATSAPP", "Utility", "Invoice {{1}}",
                        List.of("1"), "en", true));
    }

    private void stubVaccineTemplate() {
        when(kittypWhatsAppMessaging.requireApprovedTextTemplate("vaccine_reminder", "en"))
                .thenReturn(new com.kittyp.notification.model.MessagingDtos.ApprovedTemplate(
                        "vaccine_reminder__en", "vaccine_reminder", "WHATSAPP", "Utility",
                        "Hello {{1}}, {{2}} is due for {{3}}.", List.of("1", "2", "3"), "en"));
    }

    @Test
    void requireSenderReadyFailsWhenCredentialsMissing() {
        when(whatsAppService.isDeliveryEnabled()).thenReturn(true);
        when(kittypWhatsAppMessaging.requireSender()).thenThrow(new CustomException(
                "Kittyp WhatsApp sender is not configured", HttpStatus.SERVICE_UNAVAILABLE));
        CustomException ex = assertThrows(CustomException.class,
                () -> outboundMessageService.requireSenderReady());
        assertEquals(HttpStatus.SERVICE_UNAVAILABLE, ex.getHttpStatus());
        assertTrue(ex.getMessage().contains("Kittyp WhatsApp sender"));
    }

    @Test
    void requireSenderReadyFailsWhenDeliveryDisabled() {
        when(whatsAppService.isDeliveryEnabled()).thenReturn(false);
        CustomException ex = assertThrows(CustomException.class,
                () -> outboundMessageService.requireSenderReady());
        assertEquals(HttpStatus.SERVICE_UNAVAILABLE, ex.getHttpStatus());
        assertTrue(ex.getMessage().contains("WHATSAPP_ENABLED"));
    }

    @Test
    void requireSenderReadyPassesWhenCredentialsAndDeliveryOk() {
        stubSenderReady();
        outboundMessageService.requireSenderReady();
    }

    @Test
    void sendInvoiceBlockedWhenTemplateNotApproved() {
        stubSenderReady();
        when(kittypWhatsAppMessaging.requireInvoiceDocumentTemplate("invoice", "en"))
                .thenThrow(new CustomException(
                        "Kittyp's WhatsApp invoice document template is not approved",
                        HttpStatus.SERVICE_UNAVAILABLE));
        CustomException ex = assertThrows(CustomException.class,
                () -> outboundMessageService.sendInvoiceWhatsApp(
                        "9876543210", new byte[] {1}, "a.pdf",
                        List.of("Owner", "Clinic", "Pet", "INV-1", "100.00"), null, null));
        assertEquals(HttpStatus.SERVICE_UNAVAILABLE, ex.getHttpStatus());
        assertTrue(ex.getMessage().toLowerCase().contains("not approved"));
        verify(whatsAppService, never()).uploadDocumentPdf(any(), any(), anyString());
    }

    @Test
    void sendInvoiceUploadsPdfAndUsesConfiguredDocumentTemplate() {
        stubSenderReady();
        stubInvoiceTemplate();
        when(whatsAppService.toE164Digits("9876543210")).thenReturn("919876543210");
        when(whatsAppService.uploadDocumentPdf(eq(sender), any(), eq("INV.pdf"))).thenReturn("media-1");

        outboundMessageService.sendInvoiceWhatsApp(
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
                eq("invoice"),
                eq("en"),
                eq("media-1"),
                eq("INV.pdf"),
                eq(List.of("Owner", "Clinic", "Pet", "INV-1", "100.00")));
        verify(whatsAppService, never()).sendTextTemplate(any(), anyString(), anyString(), anyString(), anyList());
        verify(notificationLogRepository, never()).save(any());
    }

    @Test
    void sendVaccineReminderUsesTextTemplate() {
        stubSenderReady();
        stubVaccineTemplate();
        when(whatsAppService.toE164Digits(anyString())).thenReturn("919876543210");

        outboundMessageService.sendVaccineReminder(
                "9876543210", List.of("Buddy", "Rabies", "2026-09-01"), null, null);

        verify(whatsAppService).sendTextTemplate(
                eq(sender), eq("919876543210"), eq("vaccine_reminder"), eq("en"), anyList());
    }

    @Test
    void sendInvoiceDoesNotSendTemplateIfPdfUploadFails() {
        stubSenderReady();
        stubInvoiceTemplate();
        when(whatsAppService.toE164Digits(anyString())).thenReturn("919876543210");
        doThrow(new CustomException("upload failed", HttpStatus.BAD_GATEWAY))
                .when(whatsAppService).uploadDocumentPdf(eq(sender), any(), anyString());

        assertThrows(CustomException.class, () -> outboundMessageService.sendInvoiceWhatsApp(
                "9876543210",
                new byte[] {1},
                "INV.pdf",
                List.of("Owner", "Clinic", "Pet", "INV-1", "100.00"),
                null,
                null));
        verify(whatsAppService, never()).sendDocumentTemplate(
                any(), anyString(), anyString(), anyString(), anyString(), anyString(), anyList());
    }

}
