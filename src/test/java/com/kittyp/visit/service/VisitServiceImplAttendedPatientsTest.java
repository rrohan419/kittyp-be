package com.kittyp.visit.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.time.LocalDateTime;
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
import com.kittyp.clinic.entity.Clinic;
import com.kittyp.clinic.enums.ClinicStatus;
import com.kittyp.doctor.dao.DoctorProfileDao;
import com.kittyp.doctor.entity.DoctorPatientEnrollment;
import com.kittyp.doctor.entity.DoctorProfile;
import com.kittyp.doctor.repository.DoctorPatientEnrollmentRepository;
import com.kittyp.user.dao.UserDao;
import com.kittyp.user.entity.Pet;
import com.kittyp.user.entity.User;
import com.kittyp.visit.dao.VisitDao;
import com.kittyp.visit.dto.VisitDtos.AttendedPatientModel;
import com.kittyp.visit.entity.Visit;
import com.kittyp.visit.enums.VisitStatus;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class VisitServiceImplAttendedPatientsTest {

	private static final String EMAIL = "doc@test.com";
	private static final String PERSONAL_UUID = "personal-1";

	@Mock
	private VisitDao visitDao;
	@Mock
	private ClinicDao clinicDao;
	@Mock
	private DoctorProfileDao doctorProfileDao;
	@Mock
	private UserDao userDao;
	@Mock
	private DoctorPatientEnrollmentRepository doctorPatientEnrollmentRepository;

	@InjectMocks
	private VisitServiceImpl visitService;

	private User doctorUser;
	private DoctorProfile doctor;
	private Clinic personal;

	@BeforeEach
	void setUp() {
		doctorUser = User.builder().email(EMAIL).password("x").uuid("doc-user").firstName("John").lastName("Doe").build();
		doctorUser.setId(1L);
		doctor = DoctorProfile.builder().uuid("doc-1").user(doctorUser).build();
		doctor.setId(9L);
		personal = Clinic.builder().uuid(PERSONAL_UUID).name("Personal").status(ClinicStatus.VERIFIED).owner(doctorUser)
				.build();
		personal.setId(50L);
		personal.setIsActive(true);

		when(userDao.userByEmail(EMAIL)).thenReturn(doctorUser);
		when(doctorProfileDao.findByUserId(1L)).thenReturn(doctor);
		when(clinicDao.findByUuid(PERSONAL_UUID)).thenReturn(personal);
		when(doctorPatientEnrollmentRepository.findByDoctor_IdAndIsActiveTrue(9L)).thenReturn(List.of());
		when(visitDao.findByDoctor(9L)).thenReturn(List.of());
	}

	@Test
	void videoVisitWithoutClinicOwnerStillAppearsOnPersonalRoster() {
		User parent = User.builder().email("parent@test.com").password("x").uuid("parent-1").firstName("Ada")
				.lastName("Lovelace").build();
		parent.setId(2L);
		Pet pet = Pet.builder().uuid("pet-1").name("Milo").type("CAT").breed("DSH")
				.dateOfBirth(LocalDate.of(2022, 5, 12)).weight("4.2").gender("female")
				.currentFoodBrand("Hill's").allergies("fish").healthConditions("URI")
				.isNeutered(true).profilePicture("https://cdn.example/milo.jpg").build();
		pet.setId(3L);
		Visit visit = Visit.builder()
				.uuid("visit-1")
				.clinic(personal)
				.pet(pet)
				.doctor(doctor)
				.status(VisitStatus.CHECKING_OUT)
				.assessment("URI")
				.startedAt(LocalDateTime.of(2026, 8, 26, 10, 0))
				.build();
		when(visitDao.findByDoctor(9L)).thenReturn(List.of(visit));
		when(userDao.findOptionalByPetUuid("pet-1")).thenReturn(Optional.of(parent));

		List<AttendedPatientModel> rows = visitService.listMyAttendedPatients(EMAIL, PERSONAL_UUID);

		assertEquals(1, rows.size());
		assertEquals("Milo", rows.get(0).petName());
		assertEquals(LocalDate.of(2022, 5, 12), rows.get(0).dateOfBirth());
		assertEquals("4.2", rows.get(0).weight());
		assertEquals("female", rows.get(0).gender());
		assertEquals("Hill's", rows.get(0).currentFoodBrand());
		assertEquals("fish", rows.get(0).allergies());
		assertEquals("https://cdn.example/milo.jpg", rows.get(0).profilePicture());
		assertEquals(Boolean.TRUE, rows.get(0).isNeutered());
		assertEquals("Ada Lovelace", rows.get(0).ownerName());
		assertEquals("URI", rows.get(0).lastAssessment());
	}

	@Test
	void parentBookedEnrollmentAppearsOnPersonalRosterWithoutVisit() {
		User parent = User.builder().email("parent@test.com").password("x").uuid("parent-1").firstName("Ada")
				.lastName("Lovelace").build();
		parent.setId(2L);
		Pet pet = Pet.builder().uuid("pet-1").name("Milo").type("CAT").build();
		pet.setId(3L);
		DoctorPatientEnrollment enrollment = DoctorPatientEnrollment.builder()
				.doctor(doctor)
				.pet(pet)
				.ownerUser(parent)
				.build();
		enrollment.setCreatedAt(LocalDateTime.of(2026, 8, 26, 9, 0));
		when(doctorPatientEnrollmentRepository.findByDoctor_IdAndIsActiveTrue(9L)).thenReturn(List.of(enrollment));

		List<AttendedPatientModel> rows = visitService.listMyAttendedPatients(EMAIL, PERSONAL_UUID);

		assertEquals(1, rows.size());
		assertEquals("Milo", rows.get(0).petName());
		assertEquals("Ada Lovelace", rows.get(0).ownerName());
	}
}
