package com.kittyp.auth.tenant;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;

import com.kittyp.clinic.dao.ClinicDao;
import com.kittyp.clinic.dao.ClinicStaffDao;
import com.kittyp.clinic.entity.Clinic;
import com.kittyp.clinic.repository.ClinicDoctorRepository;
import com.kittyp.user.dao.UserDao;
import com.kittyp.user.entity.User;

@ExtendWith(MockitoExtension.class)
class TenantAccessServiceTest {

	@Mock
	private ClinicDao clinicDao;
	@Mock
	private ClinicStaffDao clinicStaffDao;
	@Mock
	private ClinicDoctorRepository clinicDoctorRepository;
	@Mock
	private UserDao userDao;

	private TenantAccessService service;

	@BeforeEach
	void setUp() {
		service = new TenantAccessService(clinicDao, clinicStaffDao, clinicDoctorRepository, userDao);
	}

	@Test
	void requireMember_foreignClinic_throwsAccessDenied() {
		Clinic clinic = Clinic.builder().uuid("clinic-2").build();
		clinic.setId(2L);
		User actor = User.builder().email("a@clinic1.test").password("x").build();
		actor.setId(1L);
		when(clinicDao.findByUuid("clinic-2")).thenReturn(clinic);
		when(userDao.userByEmail("a@clinic1.test")).thenReturn(actor);
		when(clinicDao.findOwnerUserId(2L)).thenReturn(99L);
		when(clinicStaffDao.isActiveMember(2L, 1L)).thenReturn(false);
		when(clinicDoctorRepository.existsByClinic_IdAndDoctor_User_IdAndIsActiveTrue(2L, 1L)).thenReturn(false);

		assertThrows(AccessDeniedException.class,
				() -> service.requireMember("clinic-2", "a@clinic1.test", "/api/v1/clinic/clinic-2/patients/p1"));
	}

	@Test
	void requireMember_owner_allows() {
		Clinic clinic = Clinic.builder().uuid("clinic-1").build();
		clinic.setId(1L);
		User actor = User.builder().email("owner@test.com").password("x").build();
		actor.setId(7L);
		when(clinicDao.findByUuid("clinic-1")).thenReturn(clinic);
		when(userDao.userByEmail("owner@test.com")).thenReturn(actor);
		when(clinicDao.findOwnerUserId(1L)).thenReturn(7L);

		service.requireMember("clinic-1", "owner@test.com", "/api/v1/clinic/clinic-1/patients/p1");
		verify(clinicDao).findOwnerUserId(1L);
	}
}
