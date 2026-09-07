package com.kittyp.visit.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;

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
import com.kittyp.user.dao.UserDao;
import com.kittyp.user.entity.User;
import com.kittyp.visit.dao.VisitDao;

@ExtendWith(MockitoExtension.class)
class VisitServiceImplClinicDayTest {

	@Mock
	private VisitDao visitDao;
	@Mock
	private ClinicDao clinicDao;
	@Mock
	private ClinicStaffDao clinicStaffDao;
	@Mock
	private ClinicDoctorRepository clinicDoctorRepository;
	@Mock
	private UserDao userDao;

	@InjectMocks
	private VisitServiceImpl visitService;

	@Test
	void listClinicVisits_defaultDayUsesClinicTimezoneNotJvm() {
		User owner = User.builder().email("admin@example.com").password("x").uuid("u-1").build();
		owner.setId(1L);
		Clinic clinic = Clinic.builder()
				.uuid("clinic-tz")
				.name("IST Clinic")
				.status(ClinicStatus.VERIFIED)
				.timezone("Asia/Kolkata")
				.owner(owner)
				.build();
		clinic.setId(7L);

		when(clinicDao.findByUuid("clinic-tz")).thenReturn(clinic);
		when(userDao.userByEmail("admin@example.com")).thenReturn(owner);
		when(visitDao.findByClinicAndDay(eq(7L), any(), any())).thenReturn(List.of());

		visitService.listClinicVisits("clinic-tz", null, null, null, null, null, "admin@example.com");

		LocalDate expected = LocalDate.now(ZoneId.of("Asia/Kolkata"));
		verify(visitDao).findByClinicAndDay(eq(7L),
				eq(expected.atStartOfDay()),
				eq(expected.atTime(java.time.LocalTime.MAX)));
	}
}
