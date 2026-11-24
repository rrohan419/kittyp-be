package com.kittyp.notification.service;

import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import com.kittyp.common.exception.CustomException;
import com.kittyp.notification.model.MessagingDtos.ApprovedTemplate;

@Service
public class KittypWhatsAppMessagingService {
    private final WhatsAppTemplateSetupService templateSetup;
    private final String token;
    private final String phoneNumberId;
    private final String businessAccountId;

    public KittypWhatsAppMessagingService(
            WhatsAppTemplateSetupService templateSetup,
            @Value("${whatsapp.token:}") String token,
            @Value("${whatsapp.phone-number-id:}") String phoneNumberId,
            @Value("${whatsapp.business-account-id:}") String businessAccountId) {
        this.templateSetup = templateSetup;
        this.token = token == null ? "" : token.trim();
        this.phoneNumberId = phoneNumberId == null ? "" : phoneNumberId.trim();
        this.businessAccountId = businessAccountId == null ? "" : businessAccountId.trim();
    }

    public WhatsAppSenderCredentials requireSender() {
        WhatsAppSenderCredentials sender = WhatsAppSenderCredentials.of(token, phoneNumberId);
        if (!sender.isConfigured()) {
            throw new CustomException(
                    "Kittyp WhatsApp sender is not configured. Set WHATSAPP_TOKEN and WHATSAPP_PHONE_NUMBER_ID.",
                    HttpStatus.SERVICE_UNAVAILABLE);
        }
        return sender;
    }

    public List<ApprovedTemplate> listApprovedTemplates() {
        if (!StringUtils.hasText(token) || !StringUtils.hasText(businessAccountId)) {
            return List.of();
        }
        return templateSetup.listApprovedMessagingTemplates(token, businessAccountId);
    }

    public ApprovedTemplate requireApprovedTemplate(String templateId) {
        return listApprovedTemplates().stream()
                .filter(template -> template.id().equals(templateId))
                .findFirst()
                .orElseThrow(() -> new CustomException(
                        "This WhatsApp template is not approved on Kittyp's WhatsApp account",
                        HttpStatus.SERVICE_UNAVAILABLE));
    }

    public ApprovedTemplate requireApprovedTextTemplate(String name, String language) {
        return listApprovedTemplates().stream()
                .filter(template -> !template.requiresDocument()
                        && template.name().equalsIgnoreCase(name)
                        && template.language().equalsIgnoreCase(language))
                .findFirst()
                .orElseThrow(() -> new CustomException(
                        "This text template is not approved on Kittyp's WhatsApp account",
                        HttpStatus.SERVICE_UNAVAILABLE));
    }

    public ApprovedTemplate requireInvoiceDocumentTemplate(String name, String language) {
        return listApprovedTemplates().stream()
                .filter(template -> template.requiresDocument()
                        && template.name().equalsIgnoreCase(name)
                        && template.language().equalsIgnoreCase(language))
                .findFirst()
                .orElseThrow(() -> new CustomException(
                        "No approved DOCUMENT template named '" + name
                                + "' was found on Kittyp's configured WhatsApp Business Account. "
                                + "Approve the invoice template on that WABA, not another account.",
                        HttpStatus.SERVICE_UNAVAILABLE));
    }
}
