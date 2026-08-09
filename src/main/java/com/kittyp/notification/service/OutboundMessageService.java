package com.kittyp.notification.service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.kittyp.common.exception.CustomException;
import com.kittyp.notification.entity.NotificationLog;
import com.kittyp.notification.enums.NotificationChannel;
import com.kittyp.notification.enums.NotificationType;
import com.kittyp.notification.repository.NotificationLogRepository;
import com.kittyp.user.entity.Pet;
import com.kittyp.user.entity.User;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Thin outbound router for WhatsApp (primary) with optional notification audit.
 * Sender credentials must come from DoctorProfile or Clinic — never cross-use.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OutboundMessageService {

    private final WhatsAppService whatsAppService;
    private final NotificationLogRepository notificationLogRepository;
    private final ObjectMapper objectMapper;

    @Value("${whatsapp.invoice-template:invoice_receipt}")
    private String invoiceTemplate;

    @Value("${whatsapp.invoice-template-lang:en}")
    private String invoiceTemplateLang;

    @Value("${whatsapp.vaccine-template:vaccine_reminder}")
    private String vaccineTemplate;

    @Value("${whatsapp.checkup-template:checkup_reminder}")
    private String checkupTemplate;

    @Value("${whatsapp.promo-template:promo_offer}")
    private String promoTemplate;

    @Value("${whatsapp.invoice-template-lang:en}")
    private String defaultLang;

    public void requireSenderReady(WhatsAppSenderCredentials sender, String ownerLabel) {
        if (!whatsAppService.isConfigured(sender)) {
            throw new CustomException(
                    "WhatsApp is not configured for this " + ownerLabel
                            + ". Add Meta Phone Number ID and token in settings.",
                    HttpStatus.SERVICE_UNAVAILABLE);
        }
    }

    public void sendInvoicePdfWhatsApp(
            WhatsAppSenderCredentials sender,
            String ownerPhone,
            byte[] pdfBytes,
            String filename,
            List<String> bodyParams,
            User auditUser,
            Pet auditPet) {
        String to = whatsAppService.toE164Digits(ownerPhone);
        String mediaId = whatsAppService.uploadDocumentPdf(sender, pdfBytes, filename);
        whatsAppService.sendDocumentTemplate(
                sender, to, invoiceTemplate, invoiceTemplateLang, mediaId, filename, bodyParams);
        audit(auditUser, auditPet, NotificationType.INVOICE_SENT, NotificationChannel.WHATSAPP, Map.of(
                "to", to,
                "template", invoiceTemplate,
                "filename", filename,
                "phoneNumberId", sender != null ? sender.phoneNumberId() : ""));
    }

    public void sendVaccineReminder(
            WhatsAppSenderCredentials sender,
            String ownerPhone,
            List<String> bodyParams,
            User auditUser,
            Pet auditPet) {
        sendText(sender, ownerPhone, vaccineTemplate, bodyParams, NotificationType.VACCINE_REMINDER, auditUser, auditPet);
    }

    public void sendCheckupReminder(
            WhatsAppSenderCredentials sender,
            String ownerPhone,
            List<String> bodyParams,
            User auditUser,
            Pet auditPet) {
        sendText(sender, ownerPhone, checkupTemplate, bodyParams, NotificationType.CHECKUP_REMINDER, auditUser, auditPet);
    }

    public void sendPromoOffer(
            WhatsAppSenderCredentials sender,
            String ownerPhone,
            List<String> bodyParams,
            User auditUser,
            Pet auditPet) {
        sendText(sender, ownerPhone, promoTemplate, bodyParams, NotificationType.PROMO_OFFER, auditUser, auditPet);
    }

    private void sendText(
            WhatsAppSenderCredentials sender,
            String ownerPhone,
            String template,
            List<String> bodyParams,
            NotificationType type,
            User auditUser,
            Pet auditPet) {
        String to = whatsAppService.toE164Digits(ownerPhone);
        whatsAppService.sendTextTemplate(sender, to, template, defaultLang, bodyParams);
        audit(auditUser, auditPet, type, NotificationChannel.WHATSAPP, Map.of(
                "to", to,
                "template", template));
    }

    @Transactional
    protected void audit(
            User user, Pet pet, NotificationType type, NotificationChannel channel, Map<String, Object> payload) {
        if (user == null) {
            log.info("WhatsApp {} sent (no linked user for NotificationLog) channel={} payload={}",
                    type, channel, payload);
            return;
        }
        try {
            notificationLogRepository.save(NotificationLog.builder()
                    .user(user)
                    .pet(pet)
                    .type(type)
                    .payload(objectMapper.writeValueAsString(Map.of(
                            "channel", channel.name(),
                            "data", payload)))
                    .sentAt(LocalDateTime.now())
                    .build());
        } catch (Exception e) {
            log.warn("Failed to persist WhatsApp notification log: {}", e.getMessage());
        }
    }
}
