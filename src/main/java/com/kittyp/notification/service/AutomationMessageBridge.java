package com.kittyp.notification.service;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.kittyp.notification.entity.PracticeMessage;
import com.kittyp.notification.enums.MessageChannel;
import com.kittyp.notification.enums.MessageStatus;
import com.kittyp.notification.enums.MessageTenantType;
import com.kittyp.notification.repository.MessageSettingsRepository;
import com.kittyp.notification.repository.PracticeMessageRepository;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class AutomationMessageBridge {
    private final PracticeMessageRepository messages;
    private final MessageSettingsRepository settings;
    private final ObjectMapper objectMapper;
    private final MessagingLifecycleService lifecycle;

    @Transactional
    public void schedule(MessageTenantType tenantType, Long tenantId, String automationId,
            String sourceType, String sourceUuid, String stage, String recipientName,
            String petName, MessageChannel channel, String recipient, String subject,
            String body, LocalDateTime scheduledAt) {
        if (tenantId == null || recipient == null || recipient.isBlank()) return;
        String idempotency = "AUTO:" + sourceType + ":" + sourceUuid + ":" + stage + ":" + channel.name();
        var existing = messages.findByTenantTypeAndTenantIdAndIdempotencyKey(
                tenantType, tenantId, idempotency);
        if (existing.isPresent()) {
            PracticeMessage message = existing.get();
            if (message.getStatus() == MessageStatus.CANCELLED) return;
            message.setScheduledAt(scheduledAt);
            message.setRecipient(recipient);
            message.setRecipientName(recipientName);
            message.setPetName(petName);
            message.setSubject(subject);
            message.setEmailBody(body);
            message.setMessageBodySnapshot(body);
            if (message.getStatus() != MessageStatus.PAUSED) {
                message.setStatus(scheduledAt.isAfter(LocalDateTime.now())
                        ? MessageStatus.SCHEDULED : MessageStatus.QUEUED);
            }
            messages.save(message);
            return;
        }
        boolean paused = settings.findByTenantTypeAndTenantId(tenantType, tenantId)
                .map(settingsValue -> Boolean.TRUE.equals(settingsValue.getPaused()))
                .orElse(false);
        if (!paused) paused = !automationEnabled(settings.findByTenantTypeAndTenantId(
                tenantType, tenantId).map(com.kittyp.notification.entity.MessageSettings::getAutomationsJson).orElse("{}"),
                automationId);
        PracticeMessage message = new PracticeMessage();
        message.setUuid(UUID.randomUUID().toString());
        message.setTenantType(tenantType);
        message.setTenantId(tenantId);
        message.setChannel(channel);
        message.setStatus(paused ? MessageStatus.PAUSED
                : scheduledAt.isAfter(LocalDateTime.now()) ? MessageStatus.SCHEDULED : MessageStatus.QUEUED);
        message.setRecipient(recipient);
        message.setRecipientName(recipientName);
        message.setPetName(petName);
        message.setSubject(subject);
        message.setEmailBody(channel == MessageChannel.EMAIL ? body : null);
        message.setMessageBodySnapshot(body);
        message.setScheduledAt(scheduledAt);
        message.setIdempotencyKey(idempotency);
        message.setAutomationId(automationId);
        message.setSourceType(sourceType);
        message.setSourceUuid(sourceUuid);
        message.setAttempts(0);
        message.setReservedCredits(0);
        message.setFreeAllowanceReserved(false);
        messages.save(message);
    }

    @Transactional
    public void cancelSource(MessageTenantType tenantType, Long tenantId, String sourceType, String sourceUuid) {
        for (PracticeMessage message : messages.findByTenantTypeAndTenantIdAndSourceTypeAndSourceUuid(
                tenantType, tenantId, sourceType, sourceUuid)) {
            if (message.getStatus() == MessageStatus.SENT || message.getStatus() == MessageStatus.DELIVERED
                    || message.getStatus() == MessageStatus.READ || message.getStatus() == MessageStatus.CANCELLED) continue;
            message.setStatus(MessageStatus.CANCELLED);
            messages.save(message);
        }
    }

    @Transactional
    public int cancelExpiredReminderAutomations() {
        return messages.cancelExpiredReminderAutomations(
                java.time.LocalDate.now(ZoneId.of("Asia/Kolkata")).atStartOfDay());
    }

    @Transactional
    public boolean canDispatch(MessageTenantType tenantType, Long tenantId, String sourceType,
            String sourceUuid, String stage, MessageChannel channel) {
        String idempotency = "AUTO:" + sourceType + ":" + sourceUuid + ":" + stage + ":" + channel.name();
        var found = messages.findByTenantTypeAndTenantIdAndIdempotencyKey(tenantType, tenantId, idempotency);
        if (found.isEmpty()) return false;
        PracticeMessage message = found.get();
        if (message.getStatus() != MessageStatus.SCHEDULED
                && message.getStatus() != MessageStatus.QUEUED
                && message.getStatus() != MessageStatus.FAILED) return false;
        boolean globallyPaused = settings.findByTenantTypeAndTenantId(tenantType, tenantId)
                .map(settingsValue -> Boolean.TRUE.equals(settingsValue.getPaused()))
                .orElse(false);
        if (globallyPaused) return false;
        String automationId = "PET_REMINDER".equals(sourceType) ? "PET_REMINDER"
                : "PET_VACCINE_SCHEDULE".equals(sourceType) ? "VACCINE_REMINDER" : null;
        if (automationId != null && !automationEnabled(settings.findByTenantTypeAndTenantId(
                tenantType, tenantId).map(com.kittyp.notification.entity.MessageSettings::getAutomationsJson).orElse("{}"),
                automationId)) return false;
        lifecycle.reserveAutomation(message);
        message.setStatus(MessageStatus.SENDING);
        message.setAttempts(message.getAttempts() + 1);
        message.setFailureCode(null);
        messages.save(message);
        return true;
    }

    @Transactional
    public void markSent(MessageTenantType tenantType, Long tenantId, String sourceType,
            String sourceUuid, String stage, MessageChannel channel, String providerMessageId) {
        PracticeMessage message = find(tenantType, tenantId, sourceType, sourceUuid, stage, channel);
        if (message == null || message.getStatus() != MessageStatus.SENDING) return;
        lifecycle.settleAutomation(message);
        message.setProviderMessageId(providerMessageId);
        message.setSentAt(LocalDateTime.now());
        message.setStatus(MessageStatus.SENT);
        messages.save(message);
    }

    @Transactional
    public void markFailed(MessageTenantType tenantType, Long tenantId, String sourceType,
            String sourceUuid, String stage, MessageChannel channel, String safeCode) {
        PracticeMessage message = find(tenantType, tenantId, sourceType, sourceUuid, stage, channel);
        if (message == null || message.getStatus() != MessageStatus.SENDING) return;
        lifecycle.releaseAutomation(message, "FAIL:" + message.getUuid() + ":" + message.getAttempts());
        message.setFailureCode(safeCode);
        message.setStatus(MessageStatus.FAILED);
        messages.save(message);
    }

    private PracticeMessage find(MessageTenantType tenantType, Long tenantId, String sourceType,
            String sourceUuid, String stage, MessageChannel channel) {
        String idempotency = "AUTO:" + sourceType + ":" + sourceUuid + ":" + stage + ":" + channel.name();
        return messages.findByTenantTypeAndTenantIdAndIdempotencyKey(
                tenantType, tenantId, idempotency).orElse(null);
    }

    private boolean automationEnabled(String json, String automationId) {
        if (json == null || json.isBlank()) return true;
        try {
            var enabled = objectMapper.readValue(json, new TypeReference<java.util.Map<String,Boolean>>() {});
            return !enabled.containsKey(automationId) || Boolean.TRUE.equals(enabled.get(automationId));
        } catch (Exception e) {
            return true;
        }
    }
}
