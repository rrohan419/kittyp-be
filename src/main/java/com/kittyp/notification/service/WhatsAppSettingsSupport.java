package com.kittyp.notification.service;

import java.util.LinkedHashMap;
import java.util.Map;

import org.springframework.util.StringUtils;

/**
 * Shared helpers for doctor/clinic WhatsApp settings responses (never expose token).
 */
public final class WhatsAppSettingsSupport {

    private WhatsAppSettingsSupport() {
    }

    public static boolean isConfigured(String phoneNumberId, String businessAccountId, String token) {
        return StringUtils.hasText(phoneNumberId)
                && StringUtils.hasText(businessAccountId)
                && StringUtils.hasText(token);
    }

    public static Map<String, Object> publicView(String phoneNumberId, String businessAccountId, String token) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("whatsappConfigured", isConfigured(phoneNumberId, businessAccountId, token));
        map.put("phoneNumberId", phoneNumberId != null ? phoneNumberId : "");
        map.put("businessAccountId", businessAccountId != null ? businessAccountId : "");
        return map;
    }

    public static Map<String, Object> publicViewWithTemplates(
            String phoneNumberId,
            String businessAccountId,
            String token,
            Map<String, Object> templateInfo) {
        Map<String, Object> map = publicView(phoneNumberId, businessAccountId, token);
        if (templateInfo != null) {
            map.putAll(templateInfo);
        }
        return map;
    }

    public static Map<String, Object> publicViewFull(
            String phoneNumberId,
            String businessAccountId,
            String token,
            String connectionStatus,
            String invoiceTemplateStatus,
            Map<String, Object> templateInfo) {
        Map<String, Object> map = publicViewWithTemplates(phoneNumberId, businessAccountId, token, templateInfo);
        map.put("connectionStatus", connectionStatus != null ? connectionStatus : WhatsAppConnectionStatuses.DISCONNECTED);
        map.put("invoiceTemplateStatus",
                invoiceTemplateStatus != null ? invoiceTemplateStatus : WhatsAppConnectionStatuses.TEMPLATE_MISSING);
        boolean ready = isConfigured(phoneNumberId, businessAccountId, token)
                && WhatsAppConnectionStatuses.isInvoiceTemplateApproved(invoiceTemplateStatus);
        map.put("whatsappReadyToSend", ready);
        return map;
    }
}
