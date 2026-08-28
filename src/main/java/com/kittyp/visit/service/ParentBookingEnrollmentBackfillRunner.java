package com.kittyp.visit.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import com.kittyp.booking.repository.BookingRepository;

import lombok.RequiredArgsConstructor;

/**
 * One-shot idempotent backfill of doctor/clinic enrollments from existing bookings.
 */
@Component
@Order(100)
@RequiredArgsConstructor
public class ParentBookingEnrollmentBackfillRunner implements ApplicationRunner {

	private static final Logger log = LoggerFactory.getLogger(ParentBookingEnrollmentBackfillRunner.class);

	private final ParentBookingEnrollmentService parentBookingEnrollmentService;

	@Override
	public void run(ApplicationArguments args) {
		try {
			int n = parentBookingEnrollmentService.backfillFromBookings();
			log.info("Parent booking enrollment backfill processed {} bookings", n);
		} catch (Exception e) {
			log.warn("Parent booking enrollment backfill skipped: {}", e.getMessage());
		}
	}
}
