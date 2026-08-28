package com.kittyp.notification.service;

import org.springframework.util.StringUtils;

/**
 * Meta Cloud API sender identity for one business WhatsApp number.
 * Doctor and clinic credentials must never be mixed on a single send.
 */
public record WhatsAppSenderCredentials(String token, String phoneNumberId) {

    public boolean isConfigured() {
        return StringUtils.hasText(token) && StringUtils.hasText(phoneNumberId);
    }

    public static WhatsAppSenderCredentials of(String token, String phoneNumberId) {
        return new WhatsAppSenderCredentials(
                token == null ? "" : token.trim(),
                phoneNumberId == null ? "" : phoneNumberId.trim());
    }
}
