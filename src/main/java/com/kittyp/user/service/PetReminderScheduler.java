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

    @Scheduled(fixedDelayString = "${kittyp.reminders.check-ms:900000}")
    public void processDue() {
        int sent = petReminderService.processDueReminders();
        if (sent > 0) {
            log.info("Dispatched {} due pet reminders", sent);
        }
    }
}
