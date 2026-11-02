package com.kittyp.user.service;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
public class PetReminderScheduler {

    private final PetReminderService petReminderService;

    @Scheduled(cron = "${kittyp.reminders.cron:0 0 10 * * *}", zone = "${kittyp.reminders.zone:Asia/Kolkata}")
    public void processDue() {
        int sent = petReminderService.processDueReminders();
        if (sent > 0) {
            log.info("Dispatched {} due pet reminders", sent);
        }
    }
}
