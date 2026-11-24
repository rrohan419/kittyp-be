package com.kittyp.notification.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

import com.kittyp.common.exception.CustomException;
import com.kittyp.notification.model.MessagingDtos.ApprovedTemplate;

class KittypWhatsAppMessagingServiceTest {
    private final WhatsAppTemplateSetupService templateSetup = mock(WhatsAppTemplateSetupService.class);
    private final KittypWhatsAppMessagingService service =
            new KittypWhatsAppMessagingService(templateSetup, "kittyp-token", "kittyp-phone", "kittyp-waba");

    @Test
    void loadsTemplatesAndSenderFromKittypAccountConfiguration() {
        ApprovedTemplate template = new ApprovedTemplate(
                "vaccine_reminder__en", "vaccine_reminder", "WHATSAPP", "Utility",
                "Hello {{1}}", List.of("1"), "en");
        when(templateSetup.listApprovedMessagingTemplates("kittyp-token", "kittyp-waba"))
                .thenReturn(List.of(template));

        assertEquals(List.of(template), service.listApprovedTemplates());
        assertEquals("kittyp-token", service.requireSender().token());
        assertEquals("kittyp-phone", service.requireSender().phoneNumberId());
        verify(templateSetup).listApprovedMessagingTemplates("kittyp-token", "kittyp-waba");
    }

    @Test
    void requiresAnApprovedDocumentTemplateFromTheGlobalAccount() {
        ApprovedTemplate textTemplate = new ApprovedTemplate(
                "checkup__en", "checkup", "WHATSAPP", "Utility", "Hello {{1}}",
                List.of("1"), "en");
        when(templateSetup.listApprovedMessagingTemplates("kittyp-token", "kittyp-waba"))
                .thenReturn(List.of(textTemplate));

        CustomException exception = assertThrows(CustomException.class,
                () -> service.requireInvoiceDocumentTemplate("invoice", "en"));

        assertEquals(HttpStatus.SERVICE_UNAVAILABLE, exception.getHttpStatus());
        assertEquals("Kittyp's WhatsApp invoice document template is not approved", exception.getMessage());
    }
}
