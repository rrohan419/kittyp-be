package com.kittyp.notification.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.kittyp.common.exception.CustomException;

import org.springframework.http.HttpStatus;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class WhatsappOtpService implements SmsService {

    private final WhatsAppService whatsAppService;

    @Value("${whatsapp.otp.template}")
    private String templateName;

    @Value("${whatsapp.otp.template-lang}")
    private String languageCode;

    @Value("${whatsapp.signup.token}")
    private String token;

    @Value("${whatsapp.signup.phone-number-id}")
    private String phoneNumberId;

    @Override
    public void sendOtp(String phoneNumber, String code) {
        if (token == null || token.isBlank() || phoneNumberId == null || phoneNumberId.isBlank()) {
            throw new CustomException(
                    "WhatsApp signup sender is not configured. Set WHATSAPP_TOKEN and WHATSAPP_PHONE_NUMBER_ID.",
                    HttpStatus.SERVICE_UNAVAILABLE);
        }

        String to = whatsAppService.toE164Digits(phoneNumber);
        whatsAppService.sendAuthenticationTemplate(
                WhatsAppSenderCredentials.of(token, phoneNumberId), to, templateName, languageCode, code);
    }

}
