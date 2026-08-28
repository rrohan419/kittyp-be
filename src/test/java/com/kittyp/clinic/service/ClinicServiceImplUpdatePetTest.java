package com.kittyp.clinic.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;

import com.kittyp.booking.dao.BookingDao;
import com.kittyp.clinic.dao.ClinicDao;
import com.kittyp.clinic.dao.ClinicStaffDao;
import com.kittyp.clinic.dto.ClinicDtos.AddOwnerPetRequest;
import com.kittyp.clinic.dto.ClinicDtos.ClinicPetListModel;
import com.kittyp.clinic.entity.Clinic;
import com.kittyp.clinic.entity.ClinicPetOwner;
import com.kittyp.clinic.enums.ClinicStatus;
import com.kittyp.clinic.repository.ClinicDoctorRepository;
import com.kittyp.clinic.repository.ClinicPetEnrollmentRepository;
import com.kittyp.common.exception.CustomException;
import com.kittyp.doctor.dao.DoctorProfileDao;
import com.kittyp.health.dao.HealthEventDao;
import com.kittyp.user.dao.UserDao;
import com.kittyp.user.entity.Pet;
import com.kittyp.user.entity.Role;
import com.kittyp.user.entity.User;
import com.kittyp.user.entity.UserRole;
import com.kittyp.user.enums.ERole;
import com.kittyp.user.repository.PetsRepository;

@ExtendWith(MockitoExtension.class)
class ClinicServiceImplUpdatePetTest {

	private static final String CLINIC_UUID = "clinic-1";
	private static final String PET_UUID = "pet-1";
	private static final String EMAIL = "clinic@example.com";

	@Mock
	private ClinicDao clinicDao;
	@Mock
	private ClinicStaffDao clinicStaffDao;
	@Mock
	private ClinicDoctorRepository clinicDoctorRepository;
	@Mock
	private UserDao userDao;
	@Mock
	private PetsRepository petsRepository;
	@Mock
	private ClinicPetEnrollmentRepository clinicPetEnrollmentRepository;
	@Mock
	private DoctorProfileDao doctorProfileDao;
	@Mock
	private BookingDao bookingDao;
	@Mock
	private HealthEventDao healthEventDao;

	@InjectMocks
	private ClinicServiceImpl clinicService;

	@Test
	void updatePet_savesAllowedFields_leavesOwnerOnlyUntouched() {
		User owner = userWithRoles(1L, EMAIL, ERole.ROLE_CLINIC_ADMIN);
		Clinic clinic = clinic(owner);
		ClinicPetOwner clinicOwner = clinicOwner(clinic);
		Pet pet = pet(clinic, clinicOwner);
		pet.setHealthConditions("CKD");
		pet.setAllergies("Chicken");
		pet.setActivityLevel("high");
		pet.setNeutered(true);
		pet.setCurrentFoodBrand("Royal Canin");
		pet.setPatientNumber("P-99");

		when(userDao.userByEmail(EMAIL)).thenReturn(owner);
		when(clinicDao.findByUuid(CLINIC_UUID)).thenReturn(clinic);
		when(clinicStaffDao.isActiveMember(clinic.getId(), owner.getId())).thenReturn(false);
		when(clinicDoctorRepository.existsByClinic_IdAndDoctor_User_IdAndIsActiveTrue(clinic.getId(), owner.getId()))
				.thenReturn(false);
		when(petsRepository.findByUuidAndClinic_Id(PET_UUID, clinic.getId())).thenReturn(Optional.of(pet));
		when(petsRepository.save(any(Pet.class))).thenAnswer(inv -> inv.getArgument(0));

		AddOwnerPetRequest request = new AddOwnerPetRequest("Milo", "Cat", "Siamese", "Female",
				LocalDate.of(2022, 3, 1), "4.2 kg", "CHIP-9", "https://cdn.example/milo.jpg", "SHOULD-NOT-APPLY");

		ClinicPetListModel model = clinicService.updatePet(CLINIC_UUID, PET_UUID, request, EMAIL);

		assertEquals("Milo", model.name());
		assertEquals("Cat", model.species());
		assertEquals("Siamese", model.breed());
		assertEquals("Female", model.gender());
		assertEquals(LocalDate.of(2022, 3, 1), model.dateOfBirth());
		assertEquals("4.2 kg", model.weight());
		assertEquals("CHIP-9", model.microchipNumber());
		assertEquals("https://cdn.example/milo.jpg", model.photoUrl());

		assertEquals("CKD", pet.getHealthConditions());
		assertEquals("Chicken", pet.getAllergies());
		assertEquals("high", pet.getActivityLevel());
		assertEquals(true, pet.isNeutered());
		assertEquals("Royal Canin", pet.getCurrentFoodBrand());
		assertEquals("P-99", pet.getPatientNumber());
		verify(petsRepository).save(pet);
	}

	@Test
	void updatePet_doctorDoesNotChangeMicrochip() {
		User clinicOwnerUser = userWithRoles(1L, EMAIL, ERole.ROLE_CLINIC_ADMIN);
		User doctor = userWithRoles(2L, "doc@example.com", ERole.ROLE_DOCTOR);
		Clinic clinic = clinic(clinicOwnerUser);
		ClinicPetOwner clinicOwner = clinicOwner(clinic);
		Pet pet = pet(clinic, clinicOwner);

		when(userDao.userByEmail(doctor.getEmail())).thenReturn(doctor);
		when(clinicDao.findByUuid(CLINIC_UUID)).thenReturn(clinic);
		when(clinicStaffDao.isActiveMember(clinic.getId(), doctor.getId())).thenReturn(false);
		when(clinicDoctorRepository.existsByClinic_IdAndDoctor_User_IdAndIsActiveTrue(clinic.getId(), doctor.getId()))
				.thenReturn(true);
		when(petsRepository.findByUuidAndClinic_Id(PET_UUID, clinic.getId())).thenReturn(Optional.of(pet));
		when(petsRepository.save(any(Pet.class))).thenAnswer(inv -> inv.getArgument(0));

		AddOwnerPetRequest request = new AddOwnerPetRequest("Milo", "Cat", "Siamese", "female",
				LocalDate.of(2022, 3, 1), "4.2 kg", "CHIP-9", "https://cdn.example/milo.jpg", null);

		ClinicPetListModel model = clinicService.updatePet(CLINIC_UUID, PET_UUID, request, doctor.getEmail());

		assertEquals("Milo", model.name());
		assertEquals("OLD-CHIP", pet.getMicrochipNumber());
		assertEquals("OLD-CHIP", model.microchipNumber());
		verify(petsRepository).save(pet);
	}

	@Test
	void updatePet_unknownPet_throwsNotFound() {
		User owner = userWithRoles(1L, EMAIL, ERole.ROLE_CLINIC_ADMIN);
		Clinic clinic = clinic(owner);
		when(userDao.userByEmail(EMAIL)).thenReturn(owner);
		when(clinicDao.findByUuid(CLINIC_UUID)).thenReturn(clinic);
		when(clinicStaffDao.isActiveMember(clinic.getId(), owner.getId())).thenReturn(false);
		when(clinicDoctorRepository.existsByClinic_IdAndDoctor_User_IdAndIsActiveTrue(clinic.getId(), owner.getId()))
				.thenReturn(false);
		when(petsRepository.findByUuidAndClinic_Id(PET_UUID, clinic.getId())).thenReturn(Optional.empty());
		when(clinicPetEnrollmentRepository.findByClinic_IdAndPet_UuidAndIsActiveTrue(clinic.getId(), PET_UUID))
				.thenReturn(Optional.empty());
		when(doctorProfileDao.findByUserId(owner.getId())).thenReturn(null);
		when(bookingDao.findByClinic(clinic.getId())).thenReturn(List.of());
		when(healthEventDao.findByClinic(clinic.getId())).thenReturn(List.of());

		CustomException ex = assertThrows(CustomException.class, () -> clinicService.updatePet(CLINIC_UUID, PET_UUID,
				new AddOwnerPetRequest("Milo", null, null, null, null, null, null, null, null), EMAIL));
		assertEquals("Pet is not a patient of this clinic.", ex.getMessage());
		verify(petsRepository, never()).save(any());
	}

	@Test
	void updatePet_noClinicAccess_throwsAccessDenied() {
		User owner = userWithRoles(1L, EMAIL, ERole.ROLE_CLINIC_ADMIN);
		User stranger = userWithRoles(2L, "other@example.com", ERole.ROLE_DOCTOR);
		Clinic clinic = clinic(owner);
		when(userDao.userByEmail(stranger.getEmail())).thenReturn(stranger);
		when(clinicDao.findByUuid(CLINIC_UUID)).thenReturn(clinic);
		when(clinicStaffDao.isActiveMember(clinic.getId(), stranger.getId())).thenReturn(false);
		when(clinicDoctorRepository.existsByClinic_IdAndDoctor_User_IdAndIsActiveTrue(clinic.getId(), stranger.getId()))
				.thenReturn(false);

		assertThrows(AccessDeniedException.class, () -> clinicService.updatePet(CLINIC_UUID, PET_UUID,
				new AddOwnerPetRequest("Milo", null, null, null, null, null, null, null, null), stranger.getEmail()));
		verify(petsRepository, never()).save(any());
	}

	private static Clinic clinic(User owner) {
		Clinic clinic = Clinic.builder()
				.uuid(CLINIC_UUID)
				.name("Branch")
				.status(ClinicStatus.VERIFIED)
				.owner(owner)
				.build();
		clinic.setId(100L);
		return clinic;
	}

	private static ClinicPetOwner clinicOwner(Clinic clinic) {
		ClinicPetOwner owner = ClinicPetOwner.builder()
				.uuid("owner-1")
				.clinic(clinic)
				.firstName("Pat")
				.lastName("Owner")
				.email("pat@example.com")
				.phone("9876543210")
				.build();
		owner.setId(10L);
		return owner;
	}

	private static Pet pet(Clinic clinic, ClinicPetOwner clinicOwner) {
		Pet pet = Pet.builder()
				.uuid(PET_UUID)
				.clinic(clinic)
				.clinicOwner(clinicOwner)
				.name("Old")
				.type("Dog")
				.breed("Mix")
				.gender("Male")
				.weight("10 kg")
				.microchipNumber("OLD-CHIP")
				.profilePicture("https://cdn.example/old.jpg")
				.patientNumber("P-99")
				.healthConditions("CKD")
				.build();
		pet.setId(5L);
		return pet;
	}

	private static User userWithRoles(Long id, String email, ERole... roles) {
		User user = User.builder().email(email).password("x").uuid("u-" + id)
				.firstName("First").lastName("Last").build();
		user.setId(id);
		Set<UserRole> userRoles = new HashSet<>();
		for (ERole eRole : roles) {
			Role role = new Role();
			role.setName(eRole);
			userRoles.add(new UserRole(user, role));
		}
		user.setUserRoles(userRoles);
		return user;
	}
}
