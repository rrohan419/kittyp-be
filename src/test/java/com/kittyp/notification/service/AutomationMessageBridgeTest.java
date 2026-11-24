package com.kittyp.notification.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.kittyp.notification.entity.PracticeMessage;
import com.kittyp.notification.enums.MessageChannel;
import com.kittyp.notification.enums.MessageStatus;
import com.kittyp.notification.enums.MessageTenantType;
import com.kittyp.notification.repository.MessageSettingsRepository;
import com.kittyp.notification.repository.PracticeMessageRepository;

@ExtendWith(MockitoExtension.class)
class AutomationMessageBridgeTest {
    @Mock private PracticeMessageRepository messages;
    @Mock private MessageSettingsRepository settings;
    @Mock private ObjectMapper objectMapper;
    @Mock private MessagingLifecycleService lifecycle;
    @InjectMocks private AutomationMessageBridge bridge;

    @Test
    void failedAutomationCanBeRetriedByItsDedicatedScheduler() {
        PracticeMessage message = new PracticeMessage();
        message.setStatus(MessageStatus.FAILED);
        message.setAttempts(1);
        message.setFailureCode("DELIVERY_FAILED");
        String idempotency = "AUTO:PET_VACCINE_SCHEDULE:VACCINE_SCHEDULE_7:ONE_DAY:WHATSAPP";
        when(messages.findByTenantTypeAndTenantIdAndIdempotencyKey(
                MessageTenantType.CLINIC, 12L, idempotency)).thenReturn(Optional.of(message));
        when(settings.findByTenantTypeAndTenantId(MessageTenantType.CLINIC, 12L)).thenReturn(Optional.empty());

        boolean claimed = bridge.canDispatch(
                MessageTenantType.CLINIC, 12L, "PET_VACCINE_SCHEDULE",
                "VACCINE_SCHEDULE_7", "ONE_DAY", MessageChannel.WHATSAPP);

        assertTrue(claimed);
        assertEquals(MessageStatus.SENDING, message.getStatus());
        assertEquals(2, message.getAttempts());
        assertNull(message.getFailureCode());
        verify(lifecycle).reserveAutomation(message);
        verify(messages).save(message);
    }

    @Test
    void automationWithoutAScheduledHistoryRecordIsNotDispatched() {
        when(messages.findByTenantTypeAndTenantIdAndIdempotencyKey(
                MessageTenantType.CLINIC, 12L,
                "AUTO:PET_VACCINE_SCHEDULE:VACCINE_SCHEDULE_7:ONE_DAY:WHATSAPP"))
                .thenReturn(Optional.empty());

        assertTrue(!bridge.canDispatch(
                MessageTenantType.CLINIC, 12L, "PET_VACCINE_SCHEDULE",
                "VACCINE_SCHEDULE_7", "ONE_DAY", MessageChannel.WHATSAPP));
    }

    @Test
    void successfulAutomationSettlesCreditsBeforeEnteringHistory() {
        PracticeMessage message = new PracticeMessage();
        message.setUuid("message-1");
        message.setTenantType(MessageTenantType.CLINIC);
        message.setTenantId(12L);
        message.setStatus(MessageStatus.SENDING);
        when(messages.findByTenantTypeAndTenantIdAndIdempotencyKey(
                MessageTenantType.CLINIC, 12L,
                "AUTO:PET_VACCINE_SCHEDULE:VACCINE_SCHEDULE_7:ONE_DAY:WHATSAPP"))
                .thenReturn(Optional.of(message));

        bridge.markSent(MessageTenantType.CLINIC, 12L, "PET_VACCINE_SCHEDULE",
                "VACCINE_SCHEDULE_7", "ONE_DAY", MessageChannel.WHATSAPP, "provider-1");

        assertEquals(MessageStatus.SENT, message.getStatus());
        assertEquals("provider-1", message.getProviderMessageId());
        verify(lifecycle).settleAutomation(message);
    }
}
