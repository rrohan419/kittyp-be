package com.kittyp.visit.service;

import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.kittyp.clinic.entity.Clinic;
import com.kittyp.clinic.entity.ClinicPetEnrollment;
import com.kittyp.clinic.entity.ClinicPetOwner;
import com.kittyp.clinic.repository.ClinicPetEnrollmentRepository;
import com.kittyp.clinic.repository.ClinicPetOwnerRepository;
import com.kittyp.clinic.service.ClinicOwnerUserLinkService;
import com.kittyp.common.constants.KeyConstant;
import com.kittyp.doctor.entity.DoctorPatientEnrollment;
import com.kittyp.doctor.entity.DoctorProfile;
import com.kittyp.doctor.repository.DoctorPatientEnrollmentRepository;
import com.kittyp.doctor.repository.DoctorProfileRepository;
import com.kittyp.user.entity.Pet;
import com.kittyp.user.entity.User;
import com.kittyp.user.repository.UserRepository;

import lombok.RequiredArgsConstructor;

/** Enrolls the same pet separately with every clinic or doctor that provides care. */
@Service
@RequiredArgsConstructor
public class ParentBookingEnrollmentService {

	private final DoctorPatientEnrollmentRepository doctorPatientEnrollmentRepository;
	private final ClinicPetEnrollmentRepository clinicPetEnrollmentRepository;
	private final ClinicPetOwnerRepository clinicPetOwnerRepository;
	private final ClinicOwnerUserLinkService clinicOwnerUserLinkService;
	private final UserRepository userRepository;
	private final DoctorProfileRepository doctorProfileRepository;

	/** Personal practice when the booked clinic is owned by the booked doctor. */
	public static boolean isPersonalPractice(Clinic clinic, DoctorProfile doctor) {
		return clinic != null && doctor != null && clinic.getOwner() != null && doctor.getUser() != null
				&& clinic.getOwner().getId().equals(doctor.getUser().getId());
	}

	@Transactional
	public void enrollAfterParentBooking(Clinic clinic, DoctorProfile doctor, Pet pet, User owner) {
		ensureClinicEnrollment(clinic, pet, owner);
		if (isPersonalPractice(clinic, doctor)) {
			ensureDoctorEnrollment(doctor, pet, owner);
		}
	}

	/**
	 * Front-desk admit: enroll an existing platform (or already-known) pet at this clinic
	 * without creating a duplicate pet row.
	 */
	@Transactional
	public void admitPetToClinic(Clinic clinic, Pet pet) {
		if (clinic == null || pet == null) {
			return;
		}
		User owner = ownerForPet(pet);
		enrollAfterStaffCare(clinic, null, pet);
		DoctorProfile practiceDoctor = clinic.getOwner() == null
				? null
				: doctorProfileRepository.findByUser_Id(clinic.getOwner().getId());
		if (owner != null && isPersonalPractice(clinic, practiceDoctor)) {
			ensureDoctorEnrollment(practiceDoctor, pet, owner);
		}
	}

	/**
	 * Staff walk-in / scheduled booking creates the clinic relationship, and adds the
	 * doctor roster relationship for personal practices.
	 */
	@Transactional
	public void enrollAfterStaffCare(Clinic clinic, DoctorProfile doctorOrNull, Pet pet) {
		if (pet == null || clinic == null) {
			return;
		}
		User owner = ownerForPet(pet);
		if (owner != null) {
			ensureClinicEnrollment(clinic, pet, owner);
		} else {
			ensureClinicEnrollmentWithPetOwner(clinic, pet);
		}
		if (doctorOrNull != null && isPersonalPractice(clinic, doctorOrNull) && owner != null) {
			ensureDoctorEnrollment(doctorOrNull, pet, owner);
		}
	}

	private void ensureClinicEnrollmentWithPetOwner(Clinic clinic, Pet pet) {
		ClinicPetOwner sourceOwner = pet.getClinicOwner();
		ClinicPetOwner clinicOwner;
		if (sourceOwner != null && sourceOwner.getClinic() != null
				&& sourceOwner.getClinic().getId().equals(clinic.getId())) {
			clinicOwner = sourceOwner;
		} else if (sourceOwner != null) {
			String email = ClinicOwnerUserLinkService.normalizeEmail(sourceOwner.getEmail());
			clinicOwner = email == null ? null : clinicPetOwnerRepository
					.findByClinic_IdAndEmailIgnoreCaseAndIsActiveTrue(clinic.getId(), email)
					.orElse(null);
			if (clinicOwner == null) {
				clinicOwner = ClinicPetOwner.builder()
						.clinic(clinic)
						.firstName(sourceOwner.getFirstName())
						.lastName(sourceOwner.getLastName())
						.email(email == null ? "owner+" + UUID.randomUUID() + "@kittyp.in" : email)
						.phone(sourceOwner.getPhone() == null || sourceOwner.getPhone().isBlank()
								? KeyConstant.PLACEHOLDER_PHONE
								: sourceOwner.getPhone())
						.alternatePhone(sourceOwner.getAlternatePhone())
						.address(sourceOwner.getAddress())
						.notes(sourceOwner.getNotes())
						.linkedUser(sourceOwner.getLinkedUser())
						.build();
				clinicOwner.setIsActive(true);
				clinicOwner = clinicPetOwnerRepository.save(clinicOwner);
			}
		} else {
			clinicOwner = ClinicPetOwner.builder()
					.clinic(clinic)
					.firstName("Client")
					.lastName("")
					.email("owner+" + UUID.randomUUID() + "@kittyp.local")
					.phone(KeyConstant.PLACEHOLDER_PHONE)
					.build();
			clinicOwner.setIsActive(true);
			clinicOwner = clinicPetOwnerRepository.save(clinicOwner);
		}
		saveClinicEnrollment(clinic, pet, clinicOwner);
	}

	private void ensureDoctorEnrollment(DoctorProfile doctor, Pet pet, User owner) {
		DoctorPatientEnrollment existing = doctorPatientEnrollmentRepository
				.findByDoctor_IdAndPet_Id(doctor.getId(), pet.getId())
				.orElse(null);
		if (existing != null) {
			existing.setIsActive(true);
			if (existing.getOwnerUser() == null || !existing.getOwnerUser().getId().equals(owner.getId())) {
				existing.setOwnerUser(owner);
			}
			doctorPatientEnrollmentRepository.save(existing);
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
		saveClinicEnrollment(clinic, pet, clinicOwner);
	}

	private void saveClinicEnrollment(Clinic clinic, Pet pet, ClinicPetOwner clinicOwner) {
		ClinicPetEnrollment enrollment = clinicPetEnrollmentRepository
				.findByClinic_IdAndPet_Id(clinic.getId(), pet.getId())
				.orElseGet(() -> ClinicPetEnrollment.builder()
						.clinic(clinic)
						.pet(pet)
						.build());
		enrollment.setClinicOwner(clinicOwner);
		enrollment.setIsActive(true);
		clinicOwner.setIsActive(true);
		clinicPetEnrollmentRepository.save(enrollment);
	}

	private User ownerForPet(Pet pet) {
		if (pet.getClinicOwner() != null && pet.getClinicOwner().getLinkedUser() != null) {
			return pet.getClinicOwner().getLinkedUser();
		}
		User owner = userRepository.findByPets_Uuid(pet.getUuid()).orElse(null);
		if (owner == null && pet.getParentUserUuid() != null && !pet.getParentUserUuid().isBlank()) {
			owner = userRepository.findByUuidIgnoreCase(pet.getParentUserUuid()).orElse(null);
		}
		return owner;
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
				phone = KeyConstant.PLACEHOLDER_PHONE;
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
