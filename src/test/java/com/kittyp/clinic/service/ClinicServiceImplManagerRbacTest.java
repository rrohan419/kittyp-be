package com.kittyp.clinic.service;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

import java.util.HashSet;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

import com.kittyp.clinic.dao.ClinicDao;
import com.kittyp.clinic.dao.ClinicStaffDao;
import com.kittyp.clinic.entity.Clinic;
import com.kittyp.clinic.enums.ClinicStatus;
import com.kittyp.clinic.repository.ClinicDoctorRepository;
import com.kittyp.common.exception.CustomException;
import com.kittyp.doctor.dao.DoctorProfileDao;
import com.kittyp.user.dao.UserDao;
import com.kittyp.user.entity.Role;
import com.kittyp.user.entity.User;
import com.kittyp.user.entity.UserRole;
import com.kittyp.user.enums.ERole;

/**
 * P0-01/02 / P1-22: clinic admin must not be weaker than staff for manage gates
 * (clients/pets/settings). Affiliated-only doctors stay out.
 */
@ExtendWith(MockitoExtension.class)
class ClinicServiceImplManagerRbacTest {

	private static final String CLINIC_UUID = "clinic-rbac";

	@Mock
	private ClinicDao clinicDao;

	@Mock
	private ClinicStaffDao clinicStaffDao;

	@Mock
	private ClinicDoctorRepository clinicDoctorRepository;

	@Mock
	private DoctorProfileDao doctorProfileDao;

	@Mock
	private UserDao userDao;

	@InjectMocks
	private ClinicServiceImpl clinicService;

	@Test
	void requireClinicManager_ownerAdmin_allowed() {
		User owner = userWithRoles(1L, "admin@example.com", ERole.ROLE_CLINIC_ADMIN);
		Clinic clinic = clinic(owner);
		when(clinicDao.findByUuid(CLINIC_UUID)).thenReturn(clinic);
		when(userDao.userByEmail(owner.getEmail())).thenReturn(owner);

		assertDoesNotThrow(() -> clinicService.requireClinicManager(CLINIC_UUID, owner.getEmail()));
	}

	@Test
	void requireClinicManager_activeStaff_allowed() {
		User owner = userWithRoles(1L, "owner@example.com", ERole.ROLE_CLINIC_ADMIN);
		User staff = userWithRoles(8L, "staff@example.com", ERole.ROLE_CLINIC_STAFF);
		Clinic clinic = clinic(owner);
		when(clinicDao.findByUuid(CLINIC_UUID)).thenReturn(clinic);
		when(userDao.userByEmail(staff.getEmail())).thenReturn(staff);
		when(clinicStaffDao.isActiveMember(clinic.getId(), staff.getId())).thenReturn(true);

		assertDoesNotThrow(() -> clinicService.requireClinicManager(CLINIC_UUID, staff.getEmail()));
	}

	@Test
	void requireClinicManager_adminStaffMember_allowedEvenWithoutDoctorAffiliation() {
		User owner = userWithRoles(1L, "owner@example.com", ERole.ROLE_CLINIC_ADMIN);
		User adminStaff = userWithRoles(9L, "prashant@example.com", ERole.ROLE_CLINIC_ADMIN);
		Clinic clinic = clinic(owner);
		when(clinicDao.findByUuid(CLINIC_UUID)).thenReturn(clinic);
		when(userDao.userByEmail(adminStaff.getEmail())).thenReturn(adminStaff);
		when(clinicStaffDao.isActiveMember(clinic.getId(), adminStaff.getId())).thenReturn(true);

		assertDoesNotThrow(() -> clinicService.requireClinicManager(CLINIC_UUID, adminStaff.getEmail()));
	}

	@Test
	void requireClinicManager_affiliatedDoctorOnly_forbidden() {
		User owner = userWithRoles(1L, "owner@example.com", ERole.ROLE_CLINIC_ADMIN);
		User doctor = userWithRoles(20L, "doc@example.com", ERole.ROLE_DOCTOR);
		Clinic clinic = clinic(owner);
		when(clinicDao.findByUuid(CLINIC_UUID)).thenReturn(clinic);
		when(userDao.userByEmail(doctor.getEmail())).thenReturn(doctor);
		when(clinicStaffDao.isActiveMember(clinic.getId(), doctor.getId())).thenReturn(false);
		when(clinicDoctorRepository.existsByClinic_IdAndDoctor_User_IdAndIsActiveTrue(clinic.getId(), doctor.getId()))
				.thenReturn(true);

		CustomException ex = assertThrows(CustomException.class,
				() -> clinicService.requireClinicManager(CLINIC_UUID, doctor.getEmail()));
		assertEquals(HttpStatus.FORBIDDEN, ex.getHttpStatus());
		assertEquals("You do not have permission to manage this clinic", ex.getMessage());
	}

	@Test
	void requireActivatedClinic_pending_rejects() {
		User owner = userWithRoles(1L, "owner@example.com", ERole.ROLE_CLINIC_ADMIN);
		Clinic clinic = clinic(owner);
		clinic.setStatus(ClinicStatus.PENDING);
		when(clinicDao.findByUuid(CLINIC_UUID)).thenReturn(clinic);
		when(userDao.userByEmail(owner.getEmail())).thenReturn(owner);
		when(clinicDoctorRepository.existsByClinic_IdAndDoctor_User_IdAndIsActiveTrue(clinic.getId(), owner.getId()))
				.thenReturn(false);
		when(doctorProfileDao.findByUserId(owner.getId())).thenReturn(null);

		CustomException ex = assertThrows(CustomException.class,
				() -> clinicService.requireActivatedClinic(CLINIC_UUID, owner.getEmail()));
		assertEquals(HttpStatus.BAD_REQUEST, ex.getHttpStatus());
		assertEquals(ClinicStatus.NOT_ACTIVATED_MESSAGE, ex.getMessage());
	}

	@Test
	void requireActivatedClinic_verified_allows() {
		User owner = userWithRoles(1L, "owner@example.com", ERole.ROLE_CLINIC_ADMIN);
		Clinic clinic = clinic(owner);
		clinic.setStatus(ClinicStatus.VERIFIED);
		when(clinicDao.findByUuid(CLINIC_UUID)).thenReturn(clinic);
		when(userDao.userByEmail(owner.getEmail())).thenReturn(owner);

		assertDoesNotThrow(() -> clinicService.requireActivatedClinic(CLINIC_UUID, owner.getEmail()));
	}

	private static Clinic clinic(User owner) {
		Clinic clinic = Clinic.builder()
				.uuid(CLINIC_UUID)
				.name("Branch")
				.status(ClinicStatus.VERIFIED)
				.owner(owner)
				.build();
		clinic.setId(40L);
		return clinic;
	}

	private static User userWithRoles(Long id, String email, ERole... roles) {
		User user = User.builder().email(email).password("x").uuid("u-" + id).build();
		user.setId(id);
		Set<UserRole> userRoles = new HashSet<>();
		for (ERole name : roles) {
			Role role = new Role();
			role.setName(name);
			UserRole ur = new UserRole();
			ur.setUser(user);
			ur.setRole(role);
			userRoles.add(ur);
		}
		user.setUserRoles(userRoles);
		return user;
	}
}
