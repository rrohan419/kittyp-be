package com.kittyp.notification.repository;

import static org.junit.jupiter.api.Assertions.assertEquals;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import com.kittyp.notification.entity.MessageSettings;
import com.kittyp.notification.entity.PracticeMessage;
import com.kittyp.notification.enums.MessageChannel;
import com.kittyp.notification.enums.MessageStatus;
import com.kittyp.notification.enums.MessageTenantType;

@DataJpaTest(properties={
        "spring.datasource.url=jdbc:h2:mem:messaging-claim;DB_CLOSE_DELAY=-1",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.jpa.hibernate.ddl-auto=create-drop"
})
class PracticeMessageRepositoryClaimTest {
    @Autowired PracticeMessageRepository messages;
    @Autowired MessageSettingsRepository settings;

    @Test void claimIsAtomicAndHonorsGlobalPause() {
        PracticeMessage m=new PracticeMessage();m.setTenantType(MessageTenantType.CLINIC);m.setTenantId(40L);
        m.setChannel(MessageChannel.WHATSAPP);m.setStatus(MessageStatus.QUEUED);m.setRecipient("919876543210");
        m.setScheduledAt(LocalDateTime.now().minusMinutes(1));m.setAttempts(0);m.setReservedCredits(1);
        m.setCreatedAt(LocalDateTime.now());
        messages.saveAndFlush(m);
        assertEquals(1,messages.claim(m.getId(),LocalDateTime.now()));
        assertEquals(0,messages.claim(m.getId(),LocalDateTime.now()));

        PracticeMessage paused=new PracticeMessage();paused.setTenantType(MessageTenantType.DOCTOR);paused.setTenantId(41L);
        paused.setChannel(MessageChannel.SMS);paused.setStatus(MessageStatus.QUEUED);paused.setRecipient("9876543210");
        paused.setScheduledAt(LocalDateTime.now().minusMinutes(1));paused.setAttempts(0);paused.setReservedCredits(1);
        paused.setCreatedAt(LocalDateTime.now());
        messages.saveAndFlush(paused);
        MessageSettings configuration=new MessageSettings();configuration.setTenantType(MessageTenantType.DOCTOR);
        configuration.setTenantId(41L);configuration.setPaused(true);configuration.setAutomationsJson("{}");
        configuration.setCreatedAt(LocalDateTime.now());
        settings.saveAndFlush(configuration);
        assertEquals(0,messages.claim(paused.getId(),LocalDateTime.now()));
    }

    @Test void genericDispatcherCannotClaimOrSelectAutomationMessages() {
        PracticeMessage automated=new PracticeMessage();
        automated.setTenantType(MessageTenantType.CLINIC);automated.setTenantId(42L);
        automated.setChannel(MessageChannel.WHATSAPP);automated.setStatus(MessageStatus.QUEUED);
        automated.setRecipient("919876543210");automated.setScheduledAt(LocalDateTime.now().minusMinutes(1));
        automated.setAttempts(0);automated.setReservedCredits(1);automated.setCreatedAt(LocalDateTime.now());
        automated.setAutomationId("VACCINE_REMINDER");
        messages.saveAndFlush(automated);

        assertEquals(0,messages.claim(automated.getId(),LocalDateTime.now()));
        assertEquals(List.of(), messages
                .findTop100ByAutomationIdIsNullAndStatusInAndScheduledAtLessThanEqualOrderByScheduledAtAsc(
                List.of(MessageStatus.QUEUED,MessageStatus.SCHEDULED),LocalDateTime.now()));
    }

    @Test void expiredReminderAutomationsAreCancelledButTodaysStageRemainsQueued() {
        PracticeMessage expired=automationMessage("VACCINE_REMINDER", LocalDateTime.now().minusDays(1));
        PracticeMessage today=automationMessage("VACCINE_REMINDER", LocalDateTime.now().toLocalDate().atStartOfDay());
        messages.saveAllAndFlush(List.of(expired,today));

        assertEquals(1,messages.cancelExpiredReminderAutomations(
                LocalDateTime.now().toLocalDate().atStartOfDay()));
        assertEquals(MessageStatus.CANCELLED,messages.findById(expired.getId()).orElseThrow().getStatus());
        assertEquals(MessageStatus.QUEUED,messages.findById(today.getId()).orElseThrow().getStatus());
    }

    private static PracticeMessage automationMessage(String automationId, LocalDateTime scheduledAt) {
        PracticeMessage message=new PracticeMessage();
        message.setTenantType(MessageTenantType.CLINIC);message.setTenantId(43L);
        message.setChannel(MessageChannel.WHATSAPP);message.setStatus(MessageStatus.QUEUED);
        message.setRecipient("919876543210");message.setScheduledAt(scheduledAt);
        message.setAttempts(0);message.setReservedCredits(1);message.setCreatedAt(LocalDateTime.now());
        message.setAutomationId(automationId);
        return message;
    }
}
