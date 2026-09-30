package com.kittyp.booking.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.kittyp.booking.entity.Booking;
import com.kittyp.booking.enums.BookingStatus;
import com.kittyp.booking.repository.BookingRepository;
import com.kittyp.clinic.entity.Clinic;
import com.kittyp.email.service.ZeptoMailService;
import com.kittyp.user.entity.User;

@ExtendWith(MockitoExtension.class)
class AppointmentReminderServiceTest {

	@Mock
	private BookingRepository bookingRepository;
	@Mock
	private ZeptoMailService zeptoMailService;
	@InjectMocks
	private AppointmentReminderService service;

	@Test
	void skipsWhenReminderAlreadyClaimed() {
		LocalDateTime now = LocalDateTime.of(2026, 10, 1, 10, 0);
		Booking booking = openBooking(now.plusMinutes(30));
		when(bookingRepository.findDueForReminder(any(), any(), any())).thenReturn(List.of(booking));
		when(bookingRepository.claimReminder(any(), any(), any())).thenReturn(0);

		assertEquals(0, service.processDueReminders(now, fixed(now)));
		verify(zeptoMailService, never()).sendAppointmentReminderEmail(any(), any(), any(), any(), any(), any(),
				any(), any(), any(), any());
	}

	@Test
	void skipsCancelledAfterClaim() {
		LocalDateTime now = LocalDateTime.of(2026, 10, 1, 10, 0);
		Booking booking = openBooking(now.plusMinutes(30));
		Booking cancelled = openBooking(now.plusMinutes(30));
		cancelled.setStatus(BookingStatus.CANCELLED);
		cancelled.setReminderSentAt(now);
		when(bookingRepository.findDueForReminder(any(), any(), any())).thenReturn(List.of(booking));
		when(bookingRepository.claimReminder(any(), any(), any())).thenReturn(1);
		when(bookingRepository.findByUuid("book-1")).thenReturn(Optional.of(cancelled));

		assertEquals(0, service.processDueReminders(now, fixed(now)));
		verify(zeptoMailService, never()).sendAppointmentReminderEmail(any(), any(), any(), any(), any(), any(),
				any(), any(), any(), any());
		verify(bookingRepository, never()).releaseReminder(any(), any());
	}

	@Test
	void sendsWhenClaimedAndStillOpen() {
		LocalDateTime now = LocalDateTime.of(2026, 10, 1, 10, 0);
		Booking booking = openBooking(now.plusMinutes(30));
		Booking claimed = openBooking(now.plusMinutes(30));
		claimed.setReminderSentAt(now);
		claimed.getClinic().setLatitude(18.52);
		claimed.getClinic().setLongitude(73.85);
		when(bookingRepository.findDueForReminder(any(), any(), any())).thenReturn(List.of(booking));
		when(bookingRepository.claimReminder(any(), any(), any())).thenReturn(1);
		when(bookingRepository.findByUuid("book-1")).thenReturn(Optional.of(claimed));
		when(zeptoMailService.sendAppointmentReminderEmail(any(), any(), any(), any(), any(), any(), any(), any(),
				any(), any())).thenReturn(true);

		assertEquals(1, service.processDueReminders(now, fixed(now)));
		verify(zeptoMailService).sendAppointmentReminderEmail(any(), any(), any(), any(), any(), any(), any(),
				any(), any(), eq("https://www.google.com/maps/search/?api=1&query=18.52,73.85"));
		verify(bookingRepository, never()).releaseReminder(any(), any());
	}

	@Test
	void doesNotClaimWhenOwnerHasNoEmail() {
		LocalDateTime now = LocalDateTime.of(2026, 10, 1, 10, 0);
		Booking booking = openBooking(now.plusMinutes(30));
		booking.setOwner(null);
		when(bookingRepository.findDueForReminder(any(), any(), any())).thenReturn(List.of(booking));

		assertEquals(0, service.processDueReminders(now, fixed(now)));
		verify(bookingRepository, never()).claimReminder(any(), any(), any());
	}

	@Test
	void releasesClaimWhenZeptoDoesNotAccept() {
		LocalDateTime now = LocalDateTime.of(2026, 10, 1, 10, 0);
		Booking booking = openBooking(now.plusMinutes(30));
		Booking claimed = openBooking(now.plusMinutes(30));
		claimed.setReminderSentAt(now);
		when(bookingRepository.findDueForReminder(any(), any(), any())).thenReturn(List.of(booking));
		when(bookingRepository.claimReminder(any(), any(), any())).thenReturn(1);
		when(bookingRepository.findByUuid("book-1")).thenReturn(Optional.of(claimed));
		when(zeptoMailService.sendAppointmentReminderEmail(any(), any(), any(), any(), any(), any(), any(), any(),
				any(), any())).thenReturn(false);

		assertEquals(0, service.processDueReminders(now, fixed(now)));
		verify(bookingRepository).releaseReminder(eq(1L), any());
	}

	@Test
	void slotInsideClinicHourIsDue() {
		LocalDateTime clinicNow = LocalDateTime.of(2026, 10, 1, 10, 0);
		Booking booking = openBooking(clinicNow.plusMinutes(30));
		booking.setTimezone("Asia/Kolkata");
		Booking claimed = openBooking(clinicNow.plusMinutes(30));
		claimed.setTimezone("Asia/Kolkata");
		claimed.setReminderSentAt(clinicNow);
		when(bookingRepository.findDueForReminder(any(), any(), any())).thenReturn(List.of(booking));
		when(bookingRepository.claimReminder(any(), any(), any())).thenReturn(1);
		when(bookingRepository.findByUuid("book-1")).thenReturn(Optional.of(claimed));
		when(zeptoMailService.sendAppointmentReminderEmail(any(), any(), any(), any(), any(), any(), any(), any(),
				any(), any())).thenReturn(true);

		assertEquals(1, service.processDueReminders(clinicNow, fixed(clinicNow)));
	}

	@Test
	void slotSixHoursAheadInClinicZoneIsNotDue() {
		LocalDateTime sqlCenter = LocalDateTime.of(2026, 10, 1, 10, 0);
		Booking booking = openBooking(sqlCenter.plusMinutes(30));
		booking.setTimezone("Asia/Kolkata");
		when(bookingRepository.findDueForReminder(any(), any(), any())).thenReturn(List.of(booking));

		assertEquals(0, service.processDueReminders(sqlCenter, fixed(sqlCenter.plusHours(6))));
		verify(bookingRepository, never()).claimReminder(any(), any(), any());
	}

	private static Function<String, LocalDateTime> fixed(LocalDateTime clock) {
		return zone -> clock;
	}

	private static Booking openBooking(LocalDateTime slotStart) {
		User parent = User.builder().email("parent@kittyp.test").firstName("Ada").build();
		Clinic clinic = Clinic.builder().name("Clinic").phone("9999999999").timezone("Asia/Kolkata").build();
		Booking booking = Booking.builder()
				.uuid("book-1")
				.owner(parent)
				.clinic(clinic)
				.status(BookingStatus.CONFIRMED)
				.slotStart(slotStart)
				.timezone("Asia/Kolkata")
				.build();
		booking.setId(1L);
		booking.setIsActive(true);
		return booking;
	}
}
