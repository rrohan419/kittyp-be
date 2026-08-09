package com.kittyp.notification.service;

import java.util.List;

/**
 * Meta WhatsApp Cloud API delivery for business-initiated messages (templates + media).
 */
public interface WhatsAppService {

    boolean isConfigured(WhatsAppSenderCredentials sender);

    /**
     * Normalize to digits-only E.164 without leading '+', e.g. 919876543210.
     * 10-digit numbers get the default country code prepended.
     */
    String toE164Digits(String rawPhone);

    /**
     * Upload a PDF and return Meta media id using the given sender credentials.
     */
    String uploadDocumentPdf(WhatsAppSenderCredentials sender, byte[] pdfBytes, String filename);

    /**
     * Send an approved template with DOCUMENT header (media id) and body text params.
     */
    void sendDocumentTemplate(
            WhatsAppSenderCredentials sender,
            String toE164Digits,
            String templateName,
            String languageCode,
            String mediaId,
            String filename,
            List<String> bodyParams);

    /**
     * Send an approved text-only template (vaccine / checkup / offers).
     */
    void sendTextTemplate(
            WhatsAppSenderCredentials sender,
            String toE164Digits,
            String templateName,
            String languageCode,
            List<String> bodyParams);
}
