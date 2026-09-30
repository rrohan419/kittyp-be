package com.kittyp.booking.service;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
public class AppointmentReminderScheduler {

	private final AppointmentReminderService appointmentReminderService;

	@Scheduled(fixedDelayString = "${kittyp.appointment.reminder.check-ms:300000}")
	public void processDue() {
		int sent = appointmentReminderService.processDueReminders();
		if (sent > 0) {
			log.info("Dispatched {} appointment reminders", sent);
		}
	}
}
