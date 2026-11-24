package com.kittyp.notification.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.kittyp.common.exception.CustomException;
import com.kittyp.doctor.repository.DoctorProfileRepository;
import com.kittyp.email.dto.ZeptoMailDto;
import com.kittyp.email.emailsender.IEmailSender;
import com.kittyp.email.model.ZeptoMailResponseModel;
import com.kittyp.notification.entity.PracticeMessage;
import com.kittyp.notification.enums.MessageChannel;
import com.kittyp.notification.enums.MessageTenantType;
import com.kittyp.notification.model.MessagingDtos.ApprovedTemplate;

class PracticeMessageDeliveryTest {
    private final Msg91FlowSmsProvider smsProvider = mock(Msg91FlowSmsProvider.class);
    private final WhatsAppService whatsAppService = mock(WhatsAppService.class);
    @SuppressWarnings("unchecked")
    private final IEmailSender<ZeptoMailDto, ZeptoMailResponseModel> emailSender =
            mock(IEmailSender.class);
    private final MessagingTemplateCatalogService templateCatalog = mock(MessagingTemplateCatalogService.class);
    private final KittypWhatsAppMessagingService kittypWhatsAppMessaging =
            mock(KittypWhatsAppMessagingService.class);
    private final DoctorProfileRepository doctorProfiles = mock(DoctorProfileRepository.class);
    private final WhatsAppSenderCredentials practiceSender =
            WhatsAppSenderCredentials.of("practice-token", "practice-phone-id");
    private PracticeMessageDelivery delivery;

    @BeforeEach
    void setUp() {
        delivery = new PracticeMessageDelivery(smsProvider, whatsAppService, emailSender,
                new ObjectMapper(), templateCatalog, kittypWhatsAppMessaging, doctorProfiles);
    }

    @Test
    void sendsApprovedTemplateThroughThePracticeWabaWithOrderedParameters() {
        ApprovedTemplate template = new ApprovedTemplate(
                "vaccine_reminder__en", "vaccine_reminder", "WHATSAPP", "Utility",
                "Hello {{1}}, {{2}} is due for {{4}} on {{5}} from {{3}}.",
                List.of("1", "2", "3", "4", "5"), "en");
        when(templateCatalog.require(MessageChannel.WHATSAPP, template.id()))
                .thenReturn(template);
        when(kittypWhatsAppMessaging.requireSender()).thenReturn(practiceSender);
        when(whatsAppService.isDeliveryEnabled()).thenReturn(true);
        when(whatsAppService.isConfigured(practiceSender)).thenReturn(true);
        when(whatsAppService.toE164Digits("+919876543210")).thenReturn("919876543210");
        when(whatsAppService.sendTextTemplateWithId(
                eq(practiceSender), eq("919876543210"), eq("vaccine_reminder"), eq("en"), anyList()))
                .thenReturn("wamid-practice");

        PracticeMessage message = message(template.id(),
                "{\"1\":\"Alex\",\"2\":\"Buddy\",\"3\":\"KittyP Clinic\",\"4\":\"vaccination\",\"5\":\"12 Sep\"}");

        assertEquals("wamid-practice", delivery.deliver(message));
        verify(whatsAppService).sendTextTemplateWithId(
                practiceSender, "919876543210", "vaccine_reminder", "en",
                List.of("Alex", "Buddy", "KittyP Clinic", "vaccination", "12 Sep"));
    }

    @Test
    void documentTemplatesCannotBeSentWithoutTheirBillingPdf() {
        ApprovedTemplate template = new ApprovedTemplate(
                "invoice__en", "invoice", "WHATSAPP", "Utility", "Invoice {{1}}",
                List.of("1"), "en", true);
        when(templateCatalog.require(MessageChannel.WHATSAPP, template.id()))
                .thenReturn(template);
        when(whatsAppService.isDeliveryEnabled()).thenReturn(true);

        assertThrows(CustomException.class, () -> delivery.deliver(message(template.id(), "{\"1\":\"INV-1\"}")));
        verify(whatsAppService).isDeliveryEnabled();
        verify(whatsAppService, never()).sendTextTemplateWithId(
                eq(practiceSender), anyString(), anyString(), anyString(), anyList());
        verifyNoInteractions(kittypWhatsAppMessaging);
    }

    private PracticeMessage message(String templateId, String variables) {
        PracticeMessage message = new PracticeMessage();
        message.setTenantType(MessageTenantType.CLINIC);
        message.setTenantId(42L);
        message.setChannel(MessageChannel.WHATSAPP);
        message.setRecipient("+919876543210");
        message.setTemplateKey(templateId);
        message.setTemplateVariables(variables);
        return message;
    }
}
