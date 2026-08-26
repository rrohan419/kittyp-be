package com.kittyp.clinic.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.kittyp.clinic.dao.ClinicDao;
import com.kittyp.clinic.dao.ClinicStaffDao;
import com.kittyp.clinic.dto.ClinicDtos.ClinicModel;
import com.kittyp.clinic.entity.Clinic;
import com.kittyp.clinic.enums.ClinicStatus;
import com.kittyp.clinic.repository.ClinicDoctorRepository;
import com.kittyp.doctor.dao.DoctorProfileDao;
import com.kittyp.user.dao.UserDao;
import com.kittyp.user.entity.Role;
import com.kittyp.user.entity.User;
import com.kittyp.user.entity.UserRole;
import com.kittyp.user.enums.ERole;

@ExtendWith(MockitoExtension.class)
class ClinicServiceImplPersonalFlagTest {

	private static final String CLINIC_UUID = "clinic-own";

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
	void mine_clinicAdminOwnerWithoutDoctorAffiliation_personalFalse() {
		User owner = userWithRoles(1L, "clinic@example.com", ERole.ROLE_CLINIC_ADMIN);
		Clinic clinic = clinic(owner);
		stubMineMembership(owner, clinic);
		when(clinicDoctorRepository.existsByClinic_IdAndDoctor_User_IdAndIsActiveTrue(clinic.getId(), owner.getId()))
				.thenReturn(false);

		List<ClinicModel> mine = clinicService.mine(owner.getEmail());

		assertEquals(1, mine.size());
		assertFalse(Boolean.TRUE.equals(mine.get(0).personal()));
	}

	@Test
	void mine_doctorOwnerWithAffiliation_personalTrue() {
		User owner = userWithRoles(2L, "doc@example.com", ERole.ROLE_DOCTOR);
		Clinic clinic = clinic(owner);
		stubMineMembership(owner, clinic);
		when(clinicDoctorRepository.existsByClinic_IdAndDoctor_User_IdAndIsActiveTrue(clinic.getId(), owner.getId()))
				.thenReturn(true);

		List<ClinicModel> mine = clinicService.mine(owner.getEmail());

		assertEquals(1, mine.size());
		assertTrue(Boolean.TRUE.equals(mine.get(0).personal()));
	}

	private void stubMineMembership(User owner, Clinic clinic) {
		when(userDao.userByEmail(owner.getEmail())).thenReturn(owner);
		when(clinicDao.findAllByOwnerUserId(owner.getId())).thenReturn(List.of(clinic));
		when(clinicStaffDao.findActiveByUserId(owner.getId())).thenReturn(List.of());
		when(clinicDoctorRepository.findByDoctor_User_IdAndIsActiveTrue(owner.getId())).thenReturn(List.of());
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
