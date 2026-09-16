package com.kittyp.clinic.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.http.HttpStatus;

import com.kittyp.booking.dao.BookingDao;
import com.kittyp.clinic.dao.ClinicDao;
import com.kittyp.clinic.dao.ClinicStaffDao;
import com.kittyp.clinic.entity.Clinic;
import com.kittyp.clinic.repository.ClinicDoctorRepository;
import com.kittyp.clinic.repository.ClinicPetEnrollmentRepository;
import com.kittyp.common.exception.CustomException;
import com.kittyp.doctor.dao.DoctorProfileDao;
import com.kittyp.health.dao.HealthEventDao;
import com.kittyp.user.dao.UserDao;
import com.kittyp.user.entity.Pet;
import com.kittyp.user.entity.User;
import com.kittyp.user.repository.PetsRepository;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class ClinicServiceImplCrossTenantPetTest {

	private static final String CLINIC_1 = UUID.randomUUID().toString();
	private static final String PET_2 = UUID.randomUUID().toString();
	private static final String EMAIL = "owner@clinic1.test";

	@Mock
	private ClinicDao clinicDao;
	@Mock
	private ClinicStaffDao clinicStaffDao;
	@Mock
	private ClinicDoctorRepository clinicDoctorRepository;
	@Mock
	private ClinicPetEnrollmentRepository clinicPetEnrollmentRepository;
	@Mock
	private PetsRepository petsRepository;
	@Mock
	private DoctorProfileDao doctorProfileDao;
	@Mock
	private BookingDao bookingDao;
	@Mock
	private HealthEventDao healthEventDao;
	@Mock
	private UserDao userDao;

	@InjectMocks
	private ClinicServiceImpl clinicService;

	@Test
	void patientDetail_petOfClinic2OnClinic1Path_is404() {
		User owner = User.builder().email(EMAIL).password("x").build();
		owner.setId(1L);
		Clinic clinic1 = Clinic.builder().uuid(CLINIC_1).owner(owner).build();
		clinic1.setId(1L);
		Pet foreign = Pet.builder().uuid(PET_2).name("Milo").build();
		foreign.setId(9L);

		when(clinicDao.findByUuid(CLINIC_1)).thenReturn(clinic1);
		when(userDao.userByEmail(EMAIL)).thenReturn(owner);
		when(clinicStaffDao.isActiveMember(1L, 1L)).thenReturn(false);
		when(clinicDoctorRepository.existsByClinic_IdAndDoctor_User_IdAndIsActiveTrue(1L, 1L)).thenReturn(false);
		when(petsRepository.findByUuidIgnoreCase(PET_2)).thenReturn(Optional.of(foreign));
		when(petsRepository.findByUuidAndClinic_Id(PET_2, 1L)).thenReturn(Optional.empty());
		when(clinicPetEnrollmentRepository.findByClinic_IdAndPet_UuidAndIsActiveTrue(1L, PET_2))
				.thenReturn(Optional.empty());
		when(doctorProfileDao.findByUserId(1L)).thenReturn(null);
		when(bookingDao.findByClinic(1L)).thenReturn(List.of());
		when(healthEventDao.findByClinic(1L)).thenReturn(List.of());

		CustomException ex = assertThrows(CustomException.class,
				() -> clinicService.patientDetail(CLINIC_1, PET_2, EMAIL));
		assertEquals(HttpStatus.NOT_FOUND, ex.getHttpStatus());
		assertEquals("Pet is not a patient of this clinic.", ex.getMessage());
	}
}
