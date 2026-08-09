package com.kittyp.notification.service;

import org.springframework.http.HttpStatus;

import com.kittyp.common.exception.CustomException;

/**
 * Shared E.164 digit normalization for WhatsApp Cloud API {@code to} fields (no '+').
 */
public final class WhatsAppPhones {

    private WhatsAppPhones() {
    }

    /**
     * @param rawPhone            user-entered or stored phone
     * @param defaultCountryCode  digits only, e.g. {@code 91}
     * @return digits-only E.164 without leading '+', e.g. {@code 919876543210}
     */
    public static String toE164Digits(String rawPhone, String defaultCountryCode) {
        if (rawPhone == null || rawPhone.isBlank()) {
            throw new CustomException("Owner phone is required to send WhatsApp", HttpStatus.BAD_REQUEST);
        }
        String country = defaultCountryCode == null || defaultCountryCode.isBlank()
                ? "91"
                : defaultCountryCode.replace("+", "").replaceAll("\\D", "");
        if (country.isEmpty()) {
            country = "91";
        }

        String digits = rawPhone.replaceAll("\\D", "");
        if (digits.isEmpty()) {
            throw new CustomException("Owner phone is invalid", HttpStatus.BAD_REQUEST);
        }
        // International dial prefix 00…
        if (digits.startsWith("00") && digits.length() > 2) {
            digits = digits.substring(2);
        }
        // Local trunk 0 + 10-digit mobile (common IN store format)
        if (digits.length() == 11 && digits.startsWith("0")) {
            digits = digits.substring(1);
        }
        if (digits.length() == 10) {
            digits = country + digits;
        }
        if (digits.length() < 11 || digits.length() > 15) {
            throw new CustomException("Owner phone must be a valid mobile number", HttpStatus.BAD_REQUEST);
        }
        return digits;
    }

    /** Meta template text params reject newlines/tabs. */
    public static String sanitizeTemplateText(String value) {
        if (value == null || value.isBlank()) {
            return "-";
        }
        return value.replaceAll("[\\r\\n\\t]+", " ").trim();
    }

    /** Log-safe phone: mask all but last 4 digits. */
    public static String redact(String phone) {
        if (phone == null || phone.isBlank()) {
            return "***";
        }
        String digits = phone.replaceAll("\\D", "");
        if (digits.length() <= 4) {
            return "***";
        }
        return "***" + digits.substring(digits.length() - 4);
    }
}
