package com.kittyp.booking.service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.EnumSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.function.Function;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.kittyp.booking.entity.Booking;
import com.kittyp.booking.enums.BookingStatus;
import com.kittyp.booking.repository.BookingRepository;
import com.kittyp.clinic.ClinicMapsLink;
import com.kittyp.clinic.entity.Clinic;
import com.kittyp.clinic.entity.ClinicPetOwner;
import com.kittyp.doctor.entity.DoctorProfile;
import com.kittyp.email.service.ZeptoMailService;
import com.kittyp.user.entity.Pet;
import com.kittyp.user.entity.User;
import com.kittyp.visit.service.AppointmentChangeWindow;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class AppointmentReminderService {

	static final Set<BookingStatus> OPEN = EnumSet.of(BookingStatus.PENDING, BookingStatus.CONFIRMED);
	private static final DateTimeFormatter WHEN = DateTimeFormatter.ofPattern("d MMM yyyy, h:mm a", Locale.ENGLISH);

	private final BookingRepository bookingRepository;
	private final ZeptoMailService zeptoMailService;

	@Transactional
	public int processDueReminders() {
		return processDueReminders(LocalDateTime.now(), AppointmentChangeWindow::now);
	}

	@Transactional
	int processDueReminders(LocalDateTime sqlCenter, Function<String, LocalDateTime> clock) {
		List<Booking> due = bookingRepository.findDueForReminder(OPEN, sqlCenter.minusHours(14),
				sqlCenter.plusHours(14));
		int sent = 0;
		for (Booking candidate : due) {
			if (candidate.getId() == null || !dueInClinic(candidate, clock)) {
				continue;
			}
			if (ownerEmail(candidate) == null) {
				log.info("Skipping reminder for booking {}: no owner email", candidate.getUuid());
				continue;
			}
			String zone = zoneOf(candidate);
			LocalDateTime clinicNow = clock.apply(zone);
			if (bookingRepository.claimReminder(candidate.getId(), clinicNow, OPEN) != 1) {
				continue;
			}
			Booking fresh = bookingRepository.findByUuid(candidate.getUuid()).orElse(null);
			if (!readyToSend(fresh) || !dueInClinic(fresh, clock)) {
				continue;
			}
			if (!send(fresh)) {
				bookingRepository.releaseReminder(fresh.getId(), OPEN);
				continue;
			}
			sent++;
		}
		return sent;
	}

	private static boolean dueInClinic(Booking booking, Function<String, LocalDateTime> clock) {
		if (booking.getSlotStart() == null) {
			return false;
		}
		LocalDateTime clinicNow = clock.apply(zoneOf(booking));
		if (clinicNow == null) {
			return false;
		}
		LocalDateTime slot = booking.getSlotStart();
		return slot.isAfter(clinicNow) && !slot.isAfter(clinicNow.plusMinutes(60));
	}

	private static String zoneOf(Booking booking) {
		Clinic clinic = booking.getClinic();
		return AppointmentChangeWindow.zone(booking.getTimezone(), clinic == null ? null : clinic.getTimezone());
	}

	private static boolean readyToSend(Booking booking) {
		if (booking == null || !Boolean.TRUE.equals(booking.getIsActive())) {
			return false;
		}
		if (booking.getReminderSentAt() == null || !OPEN.contains(booking.getStatus())) {
			return false;
		}
		return booking.getSlotStart() != null;
	}

	private boolean send(Booking booking) {
		String email = ownerEmail(booking);
		if (email == null) {
			log.info("Skipping reminder for booking {}: no owner email", booking.getUuid());
			return false;
		}
		Clinic clinic = booking.getClinic();
		Pet pet = booking.getPet();
		String clinicName = clinic != null && clinic.getName() != null ? clinic.getName() : "Clinic";
		String petName = pet != null && pet.getName() != null ? pet.getName() : "your pet";
		String phone = clinic != null && clinic.getPhone() != null ? clinic.getPhone() : "";
		return zeptoMailService.sendAppointmentReminderEmail(email, ownerName(booking), clinicName, petName,
				booking.getSlotStart().format(WHEN), doctorName(booking.getDoctor()), booking.getUuid(), phone,
				clinicAddress(clinic), ClinicMapsLink.url(clinic));
	}

	private static String clinicAddress(Clinic clinic) {
		if (clinic == null) {
			return "";
		}
		String address = clinic.getAddress() == null ? "" : clinic.getAddress().trim();
		String city = clinic.getCity() == null ? "" : clinic.getCity().trim();
		if (address.isEmpty()) {
			return city;
		}
		if (city.isEmpty()) {
			return address;
		}
		return address + ", " + city;
	}

	private static String ownerEmail(Booking booking) {
		if (booking.getOwner() != null && booking.getOwner().getEmail() != null
				&& !booking.getOwner().getEmail().isBlank()) {
			return booking.getOwner().getEmail().trim();
		}
		if (booking.getPet() != null && booking.getPet().getClinicOwner() != null
				&& booking.getPet().getClinicOwner().getEmail() != null
				&& !booking.getPet().getClinicOwner().getEmail().isBlank()) {
			return booking.getPet().getClinicOwner().getEmail().trim();
		}
		return null;
	}

	private static String ownerName(Booking booking) {
		User owner = booking.getOwner();
		if (owner != null) {
			String name = ((owner.getFirstName() == null ? "" : owner.getFirstName()) + " "
					+ (owner.getLastName() == null ? "" : owner.getLastName())).trim();
			if (!name.isBlank()) {
				return name;
			}
		}
		ClinicPetOwner clinicOwner = booking.getPet() == null ? null : booking.getPet().getClinicOwner();
		if (clinicOwner != null) {
			String name = ((clinicOwner.getFirstName() == null ? "" : clinicOwner.getFirstName()) + " "
					+ (clinicOwner.getLastName() == null ? "" : clinicOwner.getLastName())).trim();
			if (!name.isBlank()) {
				return name;
			}
		}
		return "there";
	}

	private static String doctorName(DoctorProfile doctor) {
		if (doctor == null || doctor.getUser() == null) {
			return "your veterinarian";
		}
		String name = ((doctor.getUser().getFirstName() == null ? "" : doctor.getUser().getFirstName()) + " "
				+ (doctor.getUser().getLastName() == null ? "" : doctor.getUser().getLastName())).trim();
		return name.isBlank() ? "your veterinarian" : name;
	}
}
