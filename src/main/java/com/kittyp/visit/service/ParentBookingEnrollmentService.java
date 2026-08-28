package com.kittyp.visit.service;

import java.time.LocalDate;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.kittyp.clinic.entity.Clinic;
import com.kittyp.clinic.entity.ClinicPetEnrollment;
import com.kittyp.clinic.entity.ClinicPetOwner;
import com.kittyp.clinic.repository.ClinicPetEnrollmentRepository;
import com.kittyp.clinic.repository.ClinicPetOwnerRepository;
import com.kittyp.clinic.service.ClinicOwnerUserLinkService;
import com.kittyp.doctor.entity.DoctorPatientEnrollment;
import com.kittyp.doctor.entity.DoctorProfile;
import com.kittyp.doctor.repository.DoctorPatientEnrollmentRepository;
import com.kittyp.booking.repository.BookingRepository;
import com.kittyp.user.entity.Pet;
import com.kittyp.user.entity.User;
import com.kittyp.user.repository.PetsRepository;
import com.kittyp.user.repository.UserRepository;

import lombok.RequiredArgsConstructor;

/**
 * Enrolls a parent's pet after self-serve booking: personal practice → doctor roster;
 * clinic branch → clinic CRM enrollment (same pet, multi-clinic safe).
 */
@Service
@RequiredArgsConstructor
public class ParentBookingEnrollmentService {

	private final DoctorPatientEnrollmentRepository doctorPatientEnrollmentRepository;
	private final ClinicPetEnrollmentRepository clinicPetEnrollmentRepository;
	private final ClinicPetOwnerRepository clinicPetOwnerRepository;
	private final ClinicOwnerUserLinkService clinicOwnerUserLinkService;
	private final PetsRepository petsRepository;
	private final BookingRepository bookingRepository;
	private final UserRepository userRepository;

	/** Personal practice when the booked clinic is owned by the booked doctor. */
	public static boolean isPersonalPractice(Clinic clinic, DoctorProfile doctor) {
		return clinic != null && doctor != null && clinic.getOwner() != null && doctor.getUser() != null
				&& clinic.getOwner().getId().equals(doctor.getUser().getId());
	}

	@Transactional
	public void enrollAfterParentBooking(Clinic clinic, DoctorProfile doctor, Pet pet, User owner) {
		if (isPersonalPractice(clinic, doctor)) {
			// Attach to doctor roster only — never move/clear pets.clinic_id (pet may stay at Clinic One).
			ensureDoctorEnrollment(doctor, pet, owner);
			return;
		}
		ensureClinicEnrollment(clinic, pet, owner);
	}

	/**
	 * Staff walk-in / scheduled booking: share pet with personal doctor roster or clinic CRM.
	 * Uses linked platform user when available; otherwise ensures clinic enrollment via clinic owner on the pet.
	 */
	@Transactional
	public void enrollAfterStaffCare(Clinic clinic, DoctorProfile doctorOrNull, Pet pet) {
		if (pet == null || clinic == null) {
			return;
		}
		User owner = null;
		if (pet.getClinicOwner() != null && pet.getClinicOwner().getLinkedUser() != null) {
			owner = pet.getClinicOwner().getLinkedUser();
		}
		if (owner == null) {
			owner = userRepository.findByPets_Uuid(pet.getUuid()).orElse(null);
		}
		if (owner == null && pet.getParentUserUuid() != null && !pet.getParentUserUuid().isBlank()) {
			owner = userRepository.findByUuidIgnoreCase(pet.getParentUserUuid()).orElse(null);
		}
		if (doctorOrNull != null && isPersonalPractice(clinic, doctorOrNull)) {
			if (owner != null) {
				ensureDoctorEnrollment(doctorOrNull, pet, owner);
			}
			return;
		}
		if (owner != null) {
			ensureClinicEnrollment(clinic, pet, owner);
			return;
		}
		ensureClinicEnrollmentWithPetOwner(clinic, pet);
	}

	private void ensureClinicEnrollmentWithPetOwner(Clinic clinic, Pet pet) {
		ClinicPetOwner clinicOwner = pet.getClinicOwner();
		if (clinicOwner == null) {
			return;
		}
		ClinicPetEnrollment existing = clinicPetEnrollmentRepository
				.findByClinic_IdAndPet_IdAndIsActiveTrue(clinic.getId(), pet.getId())
				.orElse(null);
		if (existing == null) {
			ClinicPetEnrollment enrollment = ClinicPetEnrollment.builder()
					.clinic(clinic)
					.pet(pet)
					.clinicOwner(clinicOwner)
					.build();
			enrollment.setIsActive(true);
			clinicPetEnrollmentRepository.save(enrollment);
		} else if (existing.getClinicOwner() == null
				|| !existing.getClinicOwner().getId().equals(clinicOwner.getId())) {
			existing.setClinicOwner(clinicOwner);
			clinicPetEnrollmentRepository.save(existing);
		}
		if (pet.getClinic() == null) {
			pet.setClinic(clinic);
			pet.setClinicOwner(clinicOwner);
			if (pet.getRegisteredAt() == null) {
				pet.setRegisteredAt(LocalDate.now());
			}
			petsRepository.save(pet);
		}
	}

	/**
	 * Idempotent backfill: personal bookings → doctor enrollment; clinic bookings → clinic enrollment.
	 */
	@Transactional
	public int backfillFromBookings() {
		int n = 0;
		for (com.kittyp.booking.entity.Booking booking : bookingRepository.findAll()) {
			if (booking == null || !Boolean.TRUE.equals(booking.getIsActive())) {
				continue;
			}
			Clinic clinic = booking.getClinic();
			DoctorProfile doctor = booking.getDoctor();
			Pet pet = booking.getPet();
			User owner = booking.getOwner();
			if (clinic == null || doctor == null || pet == null || owner == null) {
				continue;
			}
			// Touch lazy associations inside this transaction.
			clinic.getOwner();
			doctor.getUser();
			enrollAfterParentBooking(clinic, doctor, pet, owner);
			n++;
		}
		return n;
	}

	private void ensureDoctorEnrollment(DoctorProfile doctor, Pet pet, User owner) {
		DoctorPatientEnrollment existing = doctorPatientEnrollmentRepository
				.findByDoctor_IdAndPet_IdAndIsActiveTrue(doctor.getId(), pet.getId())
				.orElse(null);
		if (existing != null) {
			if (existing.getOwnerUser() == null || !existing.getOwnerUser().getId().equals(owner.getId())) {
				existing.setOwnerUser(owner);
				doctorPatientEnrollmentRepository.save(existing);
			}
			return;
		}
		DoctorPatientEnrollment enrollment = DoctorPatientEnrollment.builder()
				.doctor(doctor)
				.pet(pet)
				.ownerUser(owner)
				.build();
		enrollment.setIsActive(true);
		doctorPatientEnrollmentRepository.save(enrollment);
	}

	private void ensureClinicEnrollment(Clinic clinic, Pet pet, User owner) {
		ClinicPetOwner clinicOwner = ensureClinicPetOwner(clinic, owner);

		ClinicPetEnrollment existing = clinicPetEnrollmentRepository
				.findByClinic_IdAndPet_IdAndIsActiveTrue(clinic.getId(), pet.getId())
				.orElse(null);
		if (existing == null) {
			ClinicPetEnrollment enrollment = ClinicPetEnrollment.builder()
					.clinic(clinic)
					.pet(pet)
					.clinicOwner(clinicOwner)
					.build();
			enrollment.setIsActive(true);
			clinicPetEnrollmentRepository.save(enrollment);
		} else if (existing.getClinicOwner() == null
				|| !existing.getClinicOwner().getId().equals(clinicOwner.getId())) {
			existing.setClinicOwner(clinicOwner);
			clinicPetEnrollmentRepository.save(existing);
		}

		// First formal home clinic only — never steal from another clinic.
		if (pet.getClinic() == null) {
			pet.setClinic(clinic);
			pet.setClinicOwner(clinicOwner);
			if (pet.getRegisteredAt() == null) {
				pet.setRegisteredAt(LocalDate.now());
			}
			petsRepository.save(pet);
		}
	}

	private ClinicPetOwner ensureClinicPetOwner(Clinic clinic, User platformUser) {
		ClinicPetOwner owner = clinicPetOwnerRepository
				.findByClinic_IdAndLinkedUser_IdAndIsActiveTrue(clinic.getId(), platformUser.getId())
				.orElse(null);
		if (owner == null) {
			String ownerEmail = ClinicOwnerUserLinkService.normalizeEmail(platformUser.getEmail());
			if (ownerEmail != null) {
				owner = clinicPetOwnerRepository
						.findByClinic_IdAndEmailIgnoreCaseAndIsActiveTrue(clinic.getId(), ownerEmail)
						.orElse(null);
			}
		}
		if (owner == null) {
			String phone = ClinicOwnerUserLinkService.normalizePhoneDigits(platformUser.getPhoneNumber());
			if (phone == null || !phone.matches("\\d{10}")) {
				phone = "0000000000";
			}
			String email = ClinicOwnerUserLinkService.normalizeEmail(platformUser.getEmail());
			if (email == null || email.isBlank()) {
				email = "parent+" + platformUser.getUuid() + "@kittyp.local";
			}
			owner = ClinicPetOwner.builder()
					.clinic(clinic)
					.firstName(platformUser.getFirstName() == null || platformUser.getFirstName().isBlank()
							? "Client"
							: platformUser.getFirstName().trim())
					.lastName(platformUser.getLastName() == null ? "" : platformUser.getLastName().trim())
					.email(email)
					.phone(phone)
					.linkedUser(platformUser)
					.build();
			owner.setIsActive(true);
			owner = clinicPetOwnerRepository.save(owner);
		} else if (owner.getLinkedUser() == null) {
			owner.setLinkedUser(platformUser);
			owner = clinicPetOwnerRepository.save(owner);
		}
		return clinicOwnerUserLinkService.linkOwnerIfUserExists(owner);
	}
}
