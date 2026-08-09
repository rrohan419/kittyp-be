package com.kittyp.notification.service;

import java.util.List;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import com.kittyp.common.exception.CustomException;

import lombok.extern.slf4j.Slf4j;

/**
 * Active when WhatsApp Cloud API is disabled. Rejects real sends so FE gets a clear error.
 */
@Slf4j
@Service
@ConditionalOnProperty(name = "whatsapp.enabled", havingValue = "false", matchIfMissing = true)
public class LoggingWhatsAppService implements WhatsAppService {

    @Override
    public boolean isConfigured(WhatsAppSenderCredentials sender) {
        return false;
    }

    @Override
    public String toE164Digits(String rawPhone) {
        return WhatsAppPhones.toE164Digits(rawPhone, "91");
    }

    @Override
    public String uploadDocumentPdf(WhatsAppSenderCredentials sender, byte[] pdfBytes, String filename) {
        throw notConfigured();
    }

    @Override
    public void sendDocumentTemplate(
            WhatsAppSenderCredentials sender,
            String toE164Digits,
            String templateName,
            String languageCode,
            String mediaId,
            String filename,
            List<String> bodyParams) {
        log.warn("WhatsApp disabled — would send template {} to {}", templateName,
                WhatsAppPhones.redact(toE164Digits));
        throw notConfigured();
    }

    @Override
    public void sendTextTemplate(
            WhatsAppSenderCredentials sender,
            String toE164Digits,
            String templateName,
            String languageCode,
            List<String> bodyParams) {
        log.warn("WhatsApp disabled — would send text template {} to {}", templateName,
                WhatsAppPhones.redact(toE164Digits));
        throw notConfigured();
    }

    private CustomException notConfigured() {
        return new CustomException(
                "WhatsApp is not configured. Set WHATSAPP_ENABLED=true and Meta credentials in Doctor/Clinic settings.",
                HttpStatus.SERVICE_UNAVAILABLE);
    }
}
