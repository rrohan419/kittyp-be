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
}
