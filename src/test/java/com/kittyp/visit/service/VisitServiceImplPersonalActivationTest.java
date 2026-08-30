package com.kittyp.visit.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.kittyp.clinic.dao.ClinicDao;
import com.kittyp.clinic.dao.ClinicStaffDao;
import com.kittyp.clinic.entity.Clinic;
import com.kittyp.clinic.enums.ClinicStatus;
import com.kittyp.clinic.repository.ClinicDoctorRepository;
import com.kittyp.common.exception.CustomException;
import com.kittyp.doctor.dao.DoctorProfileDao;
import com.kittyp.doctor.entity.DoctorProfile;
import com.kittyp.user.dao.UserDao;
import com.kittyp.user.entity.User;
import com.kittyp.visit.dto.VisitDtos.WalkInCreateRequest;

@ExtendWith(MockitoExtension.class)
class VisitServiceImplPersonalActivationTest {

	@Mock
	private ClinicDao clinicDao;

	@Mock
	private ClinicStaffDao clinicStaffDao;

	@Mock
	private ClinicDoctorRepository clinicDoctorRepository;

	@Mock
	private UserDao userDao;

	@Mock
	private DoctorProfileDao doctorProfileDao;

	@InjectMocks
	private VisitServiceImpl visitService;

	@Test
	void createWalkIn_pendingBranch_rejected() {
		User staff = User.builder().email("staff@example.com").password("x").uuid("u-s").build();
		staff.setId(1L);
		Clinic clinic = Clinic.builder().uuid("branch-p").name("Branch").status(ClinicStatus.PENDING).owner(staff)
				.build();
		clinic.setId(20L);
		when(clinicDao.findByUuid("branch-p")).thenReturn(clinic);
		when(userDao.userByEmail("staff@example.com")).thenReturn(staff);

		CustomException ex = assertThrows(CustomException.class,
				() -> visitService.createWalkIn("branch-p", emptyWalkIn(), "staff@example.com"));
		assertEquals(ClinicStatus.NOT_ACTIVATED_MESSAGE, ex.getMessage());
	}

	@Test
	void createWalkIn_pendingPersonal_skipsClinicVerify() {
		User owner = User.builder().email("solo@example.com").password("x").uuid("u-o").build();
		owner.setId(2L);
		Clinic clinic = Clinic.builder().uuid("personal-p").name("NewTrition").status(ClinicStatus.PENDING).owner(owner)
				.build();
		clinic.setId(21L);
		when(clinicDao.findByUuid("personal-p")).thenReturn(clinic);
		when(userDao.userByEmail("solo@example.com")).thenReturn(owner);
		when(clinicDoctorRepository.existsByClinic_IdAndDoctor_User_IdAndIsActiveTrue(21L, 2L)).thenReturn(true);

		CustomException ex = assertThrows(CustomException.class,
				() -> visitService.createWalkIn("personal-p", emptyWalkIn(), "solo@example.com"));
		assertEquals("Provide petUuid or owner + newPet for walk-in", ex.getMessage());
	}

	@Test
	void createWalkIn_pendingPersonal_ownerNotLoaded_skipsClinicVerify() {
		User owner = User.builder().email("solo@example.com").password("x").uuid("u-o").build();
		owner.setId(2L);
		Clinic clinic = Clinic.builder().uuid("personal-p").name("NewTrition").status(ClinicStatus.PENDING).owner(null)
				.build();
		clinic.setId(21L);
		when(clinicDao.findByUuid("personal-p")).thenReturn(clinic);
		when(userDao.userByEmail("solo@example.com")).thenReturn(owner);
		when(clinicDao.findOwnerUserId(21L)).thenReturn(2L);
		when(clinicDoctorRepository.existsByClinic_IdAndDoctor_User_IdAndIsActiveTrue(21L, 2L)).thenReturn(true);

		CustomException ex = assertThrows(CustomException.class,
				() -> visitService.createWalkIn("personal-p", emptyWalkIn(), "solo@example.com"));
		assertEquals("Provide petUuid or owner + newPet for walk-in", ex.getMessage());
	}

	@Test
	void createWalkIn_pendingPersonal_doctorProfileWithoutAffiliation_skipsClinicVerify() {
		User owner = User.builder().email("solo@example.com").password("x").uuid("u-o").build();
		owner.setId(2L);
		Clinic clinic = Clinic.builder().uuid("personal-p").name("NewTrition").status(ClinicStatus.PENDING).owner(owner)
				.build();
		clinic.setId(21L);
		when(clinicDao.findByUuid("personal-p")).thenReturn(clinic);
		when(userDao.userByEmail("solo@example.com")).thenReturn(owner);
		when(clinicDoctorRepository.existsByClinic_IdAndDoctor_User_IdAndIsActiveTrue(21L, 2L)).thenReturn(false);
		when(doctorProfileDao.findByUserId(2L)).thenReturn(DoctorProfile.builder().uuid("doc-2").build());

		CustomException ex = assertThrows(CustomException.class,
				() -> visitService.createWalkIn("personal-p", emptyWalkIn(), "solo@example.com"));
		assertEquals("Provide petUuid or owner + newPet for walk-in", ex.getMessage());
	}

	private static WalkInCreateRequest emptyWalkIn() {
		return new WalkInCreateRequest(null, null, null, null, null, null);
	}
}
