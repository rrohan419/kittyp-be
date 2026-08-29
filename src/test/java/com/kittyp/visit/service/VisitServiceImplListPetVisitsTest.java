package com.kittyp.visit.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import com.kittyp.clinic.dao.ClinicDao;
import com.kittyp.clinic.dao.ClinicStaffDao;
import com.kittyp.clinic.entity.Clinic;
import com.kittyp.clinic.enums.ClinicStatus;
import com.kittyp.clinic.repository.ClinicDoctorRepository;
import com.kittyp.clinic.repository.ClinicPetEnrollmentRepository;
import com.kittyp.common.exception.ResourceNotFoundException;
import com.kittyp.doctor.repository.DoctorReviewRepository;
import com.kittyp.user.dao.UserDao;
import com.kittyp.user.entity.Pet;
import com.kittyp.user.entity.User;
import com.kittyp.user.repository.PetsRepository;
import com.kittyp.visit.dao.VisitDao;
import com.kittyp.visit.entity.Visit;
import com.kittyp.visit.enums.VisitStatus;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class VisitServiceImplListPetVisitsTest {

	private static final String CLINIC_UUID = "Z0HR44";
	private static final String PET_PUBLIC_ID = "9AP1AU";
	private static final String EMAIL = "clinic@test.com";

	@Mock
	private VisitDao visitDao;
	@Mock
	private ClinicDao clinicDao;
	@Mock
	private ClinicStaffDao clinicStaffDao;
	@Mock
	private ClinicDoctorRepository clinicDoctorRepository;
	@Mock
	private PetsRepository petsRepository;
	@Mock
	private UserDao userDao;
	@Mock
	private ClinicPetEnrollmentRepository clinicPetEnrollmentRepository;
	@Mock
	private DoctorReviewRepository doctorReviewRepository;

	@InjectMocks
	private VisitServiceImpl visitService;

	private Clinic clinic;
	private Pet pet;

	@BeforeEach
	void setUp() {
		User owner = User.builder().email(EMAIL).password("x").uuid("U1A2B3").firstName("Clinic").lastName("Admin")
				.build();
		owner.setId(1L);
		clinic = Clinic.builder().uuid(CLINIC_UUID).name("Branch").status(ClinicStatus.VERIFIED).owner(owner).build();
		clinic.setId(100L);
		clinic.setIsActive(true);
		pet = Pet.builder().uuid(PET_PUBLIC_ID).name("Milo").type("Cat").clinic(clinic).isNeutered(false).build();
		pet.setId(7L);
		pet.setIsActive(true);

		when(clinicDao.findByUuid(CLINIC_UUID)).thenReturn(clinic);
		when(userDao.userByEmail(EMAIL)).thenReturn(owner);
		when(clinicStaffDao.isActiveMember(100L, 1L)).thenReturn(false);
		when(clinicDoctorRepository.existsByClinic_IdAndDoctor_User_IdAndIsActiveTrue(100L, 1L)).thenReturn(false);
		when(doctorReviewRepository.findByVisit_Uuid("visit-1")).thenReturn(Optional.empty());
	}

	@Test
	void listsVisitsByPublicPetIdWhenClinicScopedUuidLookupWouldMiss() {
		when(petsRepository.findByUuidAndClinic_IdAndIsActiveTrue(PET_PUBLIC_ID, 100L)).thenReturn(Optional.empty());
		when(petsRepository.findByUuidIgnoreCase(PET_PUBLIC_ID)).thenReturn(Optional.of(pet));
		Visit visit = Visit.builder().uuid("visit-1").clinic(clinic).pet(pet).status(VisitStatus.COMPLETED).build();
		when(visitDao.findByPetAndClinic(PET_PUBLIC_ID, 100L)).thenReturn(List.of(visit));

		assertEquals(1, visitService.listPetVisitsForClinic(CLINIC_UUID, PET_PUBLIC_ID, EMAIL).size());
		assertEquals(PET_PUBLIC_ID, visitService.listPetVisitsForClinic(CLINIC_UUID, PET_PUBLIC_ID, EMAIL).get(0).petUuid());
	}

	@Test
	void listsVisitsByPatientNumber() {
		when(petsRepository.findByUuidIgnoreCase("P-99")).thenReturn(Optional.empty());
		when(petsRepository.findFirstByClinic_IdAndPatientNumberIgnoreCase(100L, "P-99")).thenReturn(Optional.of(pet));
		Visit visit = Visit.builder().uuid("visit-1").clinic(clinic).pet(pet).status(VisitStatus.COMPLETED).build();
		when(visitDao.findByPetAndClinic(PET_PUBLIC_ID, 100L)).thenReturn(List.of(visit));

		assertEquals(1, visitService.listPetVisitsForClinic(CLINIC_UUID, "P-99", EMAIL).size());
	}

	@Test
	void unknownPublicIdStill404s() {
		when(petsRepository.findByUuidIgnoreCase("ZZZZZZ")).thenReturn(Optional.empty());
		when(petsRepository.findFirstByClinic_IdAndPatientNumberIgnoreCase(100L, "ZZZZZZ")).thenReturn(Optional.empty());

		assertThrows(ResourceNotFoundException.class,
				() -> visitService.listPetVisitsForClinic(CLINIC_UUID, "ZZZZZZ", EMAIL));
	}
}
