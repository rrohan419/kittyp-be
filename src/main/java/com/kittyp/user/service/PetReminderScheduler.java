package com.kittyp.user.service;

import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
public class PetReminderScheduler {

    private final PetReminderService petReminderService;
    private final com.kittyp.notification.service.AutomationMessageBridge automationMessageBridge;

    @EventListener(ApplicationReadyEvent.class)
    public void reconcileOnStartup() {
        int cancelled = automationMessageBridge.cancelExpiredReminderAutomations();
        if (cancelled > 0) {
            log.info("Cancelled {} expired reminder automation stages", cancelled);
        }
    }

    @Scheduled(cron = "${kittyp.reminders.cron:0 0 10 * * *}", zone = "${kittyp.reminders.zone:Asia/Kolkata}")
    public void processDue() {
        automationMessageBridge.cancelExpiredReminderAutomations();
        log.info("======================================Starting to process due pet reminders==================================================");
        int sent = petReminderService.processDueReminders();
        log.info("=========================================================================================Finished processing due pet reminders, dispatched {} reminders", sent);
        if (sent > 0) {
            log.info("Dispatched {} due pet reminders", sent);
        }
    }
}
