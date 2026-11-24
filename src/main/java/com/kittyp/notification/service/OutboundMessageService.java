package com.kittyp.notification.service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

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
 * All practice messages use Kittyp's centrally configured WhatsApp sender.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OutboundMessageService {

    private final WhatsAppService whatsAppService;
    private final KittypWhatsAppMessagingService kittypWhatsAppMessaging;
    private final NotificationLogRepository notificationLogRepository;
    private final ObjectMapper objectMapper;

    @Value("${whatsapp.invoice-template:invoice}")
    private String invoiceTemplate;

    @Value("${whatsapp.invoice-template-lang:en}")
    private String invoiceTemplateLang;

    @Value("${whatsapp.vaccine-template:vaccine_reminder}")
    private String vaccineTemplate;

    @Value("${whatsapp.checkup-template:checkup_reminder}")
    private String checkupTemplate;

    @Value("${whatsapp.promo-template:promo_offer}")
    private String promoTemplate;

    @Value("${whatsapp.appointment-template:}")
    private String appointmentTemplate;

    @Value("${whatsapp.invoice-template-lang:en}")
    private String defaultLang;

    public void requireSenderReady() {
        if (!whatsAppService.isDeliveryEnabled()) {
            throw new CustomException(
                    "WhatsApp sending is disabled on the server. Set WHATSAPP_ENABLED=true to deliver messages.",
                    HttpStatus.SERVICE_UNAVAILABLE);
        }
        WhatsAppSenderCredentials sender = kittypWhatsAppMessaging.requireSender();
        if (!whatsAppService.isConfigured(sender)) {
            throw new CustomException(
                    "Kittyp WhatsApp sender credentials are not configured correctly.",
                    HttpStatus.SERVICE_UNAVAILABLE);
        }
    }

    /** Invoice sends require Kittyp's configured document template to be approved by Meta. */
    public void requireInvoiceTemplateApproved() {
        requireSenderReady();
        kittypWhatsAppMessaging.requireInvoiceDocumentTemplate(
                configuredValue(invoiceTemplate, "invoice"),
                configuredValue(invoiceTemplateLang, "en"));
    }

    public void sendInvoiceWhatsApp(
            String ownerPhone,
            byte[] pdfBytes,
            String filename,
            List<String> bodyParams,
            User auditUser,
            Pet auditPet) {
        requireInvoiceTemplateApproved();
        WhatsAppSenderCredentials sender = kittypWhatsAppMessaging.requireSender();
        String to = whatsAppService.toE164Digits(ownerPhone);
        String templateName = configuredValue(invoiceTemplate, "invoice");
        String templateLanguage = configuredValue(invoiceTemplateLang, "en");
        String mediaId = whatsAppService.uploadDocumentPdf(sender, pdfBytes, filename);
        whatsAppService.sendDocumentTemplate(
                sender, to, templateName, templateLanguage, mediaId, filename, bodyParams);
        audit(auditUser, auditPet, NotificationType.INVOICE_SENT, NotificationChannel.WHATSAPP, Map.of(
                "to", to,
                "template", templateName,
                "filename", filename,
                "phoneNumberId", sender.phoneNumberId()));
    }

    private static String configuredValue(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value.trim();
    }

    public void sendVaccineReminder(
            String ownerPhone,
            List<String> bodyParams,
            User auditUser,
            Pet auditPet) {
        sendText(ownerPhone, vaccineTemplate, bodyParams, NotificationType.VACCINE_REMINDER, auditUser, auditPet);
    }

    public void sendCheckupReminder(
            String ownerPhone,
            List<String> bodyParams,
            User auditUser,
            Pet auditPet) {
        sendText(ownerPhone, checkupTemplate, bodyParams, NotificationType.CHECKUP_REMINDER, auditUser, auditPet);
    }

    public void sendPromoOffer(
            String ownerPhone,
            List<String> bodyParams,
            User auditUser,
            Pet auditPet) {
        sendText(ownerPhone, promoTemplate, bodyParams, NotificationType.PROMO_OFFER, auditUser, auditPet);
    }

    /**
     * Best-effort appointment notice. Requires the configured appointment template and never throws.
     */
    public boolean trySendAppointmentNotice(
            String phone,
            List<String> bodyParams,
            User auditUser,
            Pet auditPet) {
        if (!whatsAppService.isDeliveryEnabled() || !StringUtils.hasText(appointmentTemplate)) {
            return false;
        }
        try {
            sendText(phone, appointmentTemplate, bodyParams, NotificationType.APPOINTMENT_REMINDER,
                    auditUser, auditPet);
            return true;
        } catch (Exception e) {
            log.warn("WhatsApp appointment notice skipped: {}", e.getMessage());
            return false;
        }
    }

    private void sendText(
            String ownerPhone,
            String template,
            List<String> bodyParams,
            NotificationType type,
            User auditUser,
            Pet auditPet) {
        requireSenderReady();
        WhatsAppSenderCredentials sender = kittypWhatsAppMessaging.requireSender();
        String language = configuredValue(defaultLang, "en");
        var approvedTemplate = kittypWhatsAppMessaging.requireApprovedTextTemplate(
                configuredValue(template, ""), language);
        if (bodyParams == null || approvedTemplate.variables().size() != bodyParams.size()) {
            throw new CustomException(
                    "WhatsApp template parameter count does not match the approved Kittyp template",
                    HttpStatus.BAD_REQUEST);
        }
        String to = whatsAppService.toE164Digits(ownerPhone);
        whatsAppService.sendTextTemplate(sender, to, approvedTemplate.name(), language, bodyParams);
        audit(auditUser, auditPet, type, NotificationChannel.WHATSAPP, Map.of(
                "to", to,
                "template", approvedTemplate.name()));
    }

    @Transactional
    protected void audit(
            User user, Pet pet, NotificationType type, NotificationChannel channel, Map<String, Object> payload) {
        if (user == null) {
            log.info("WhatsApp notification sent without linked user type={} channel={}", type, channel);
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
