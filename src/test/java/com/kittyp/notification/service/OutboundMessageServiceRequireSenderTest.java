package com.kittyp.notification.service;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.kittyp.common.exception.CustomException;
import com.kittyp.notification.repository.NotificationLogRepository;

class OutboundMessageServiceRequireSenderTest {

    private WhatsAppService whatsAppService;
    private OutboundMessageService service;

    @BeforeEach
    void setUp() {
        whatsAppService = mock(WhatsAppService.class);
        service = new OutboundMessageService(
                whatsAppService, mock(NotificationLogRepository.class), new ObjectMapper());
    }

    @Test
    void missingCredentialsPointsToSettings() {
        when(whatsAppService.isConfigured(org.mockito.ArgumentMatchers.any())).thenReturn(false);
        CustomException ex = assertThrows(
                CustomException.class,
                () -> service.requireSenderReady(WhatsAppSenderCredentials.of(null, null), "doctor"));
        assertEquals(HttpStatus.SERVICE_UNAVAILABLE, ex.getHttpStatus());
        assertEquals(
                true,
                ex.getMessage().contains("Add Meta Phone Number ID and token in settings"));
    }

    @Test
    void credentialsPresentButProviderOffPointsToFeatureFlag() {
        WhatsAppSenderCredentials sender = WhatsAppSenderCredentials.of("tok", "123");
        when(whatsAppService.isConfigured(sender)).thenReturn(false);
        CustomException ex = assertThrows(
                CustomException.class, () -> service.requireSenderReady(sender, "doctor"));
        assertEquals(HttpStatus.SERVICE_UNAVAILABLE, ex.getHttpStatus());
        assertEquals(true, ex.getMessage().contains("whatsapp.enabled=true"));
    }

    @Test
    void readyWhenCredentialsAndProviderOk() {
        WhatsAppSenderCredentials sender = WhatsAppSenderCredentials.of("tok", "123");
        when(whatsAppService.isConfigured(sender)).thenReturn(true);
        assertDoesNotThrow(() -> service.requireSenderReady(sender, "doctor"));
    }
}
