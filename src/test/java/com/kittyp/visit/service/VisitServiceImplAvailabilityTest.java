package com.kittyp.visit.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.kittyp.booking.entity.DoctorAvailability;
import com.kittyp.booking.repository.DoctorAvailabilityRepository;
import com.kittyp.clinic.dao.ClinicDao;
import com.kittyp.clinic.dao.ClinicStaffDao;
import com.kittyp.clinic.entity.Clinic;
import com.kittyp.clinic.entity.ClinicDoctor;
import com.kittyp.clinic.enums.ClinicStatus;
import com.kittyp.clinic.repository.ClinicDoctorRepository;
import com.kittyp.common.exception.CustomException;
import com.kittyp.doctor.dao.DoctorProfileDao;
import com.kittyp.doctor.entity.DoctorProfile;
import com.kittyp.doctor.enums.DoctorStatus;
import com.kittyp.user.dao.UserDao;
import com.kittyp.user.entity.User;
import com.kittyp.visit.dto.VisitDtos.ScheduleBookingCreateRequest;
import com.kittyp.visit.dto.VisitDtos.WalkInCreateRequest;

@ExtendWith(MockitoExtension.class)
class VisitServiceImplAvailabilityTest {

	@Mock
	private ClinicDao clinicDao;

	@Mock
	private ClinicStaffDao clinicStaffDao;

	@Mock
	private ClinicDoctorRepository clinicDoctorRepository;

	@Mock
	private UserDao userDao;

	@Mock
	private DoctorAvailabilityRepository doctorAvailabilityRepository;

	@Mock
	private DoctorProfileDao doctorProfileDao;

	@InjectMocks
	private VisitServiceImpl visitService;

	@Test
	void createWalkIn_assignedDoctorOnClosedException_rejected() {
		Fixture f = stubVerifiedClinicWithDoctor();
		when(doctorAvailabilityRepository.findByDoctor_Id(5L)).thenReturn(Optional.of(closedToday(f.doctor)));

		CustomException ex = assertThrows(CustomException.class,
				() -> visitService.createWalkIn("clinic-v", walkInWithDoctor(), "staff@example.com"));
		assertEquals("Doctor is not available at this time", ex.getMessage());
	}

	@Test
	void createWalkIn_withoutDoctor_skipsHours() {
		stubVerifiedClinic();

		CustomException ex = assertThrows(CustomException.class,
				() -> visitService.createWalkIn("clinic-v", emptyWalkIn(), "staff@example.com"));
		assertEquals("Provide petUuid or owner + newPet for walk-in", ex.getMessage());
	}

	@Test
	void createScheduledBooking_exceptionDay_rejected() {
		Fixture f = stubVerifiedClinicWithDoctor();
		LocalDate day = LocalDate.now().plusDays(1);
		when(doctorAvailabilityRepository.findByDoctor_Id(5L)).thenReturn(Optional.of(closedOn(f.doctor, day)));

		ScheduleBookingCreateRequest request = new ScheduleBookingCreateRequest(
				null, null, null, "doc-1", day.atTime(10, 0), null, 30, null, null);

		CustomException ex = assertThrows(CustomException.class,
				() -> visitService.createScheduledBooking("clinic-v", request, "staff@example.com"));
		assertEquals("Doctor is not available at this time", ex.getMessage());
	}

	@Test
	void listParentDoctorSlots_exceptionDay_empty() {
		Fixture f = stubVerifiedClinicWithDoctor();
		LocalDate day = LocalDate.now().plusDays(1);
		when(doctorAvailabilityRepository.findByDoctor_Id(5L)).thenReturn(Optional.of(closedOn(f.doctor, day)));

		List<LocalDateTime> slots = visitService.listParentDoctorSlots("clinic-v", "doc-1", day, "staff@example.com");
		assertTrue(slots.isEmpty());
	}

	private Fixture stubVerifiedClinicWithDoctor() {
		Fixture f = stubVerifiedClinic();
		when(clinicDoctorRepository.findByClinic_IdAndDoctor_Uuid(30L, "doc-1"))
				.thenReturn(Optional.of(ClinicDoctor.builder().clinic(f.clinic).doctor(f.doctor).isActive(true).build()));
		return f;
	}

	private Fixture stubVerifiedClinic() {
		User staff = User.builder().email("staff@example.com").password("x").uuid("u-s").build();
		staff.setId(1L);
		Clinic clinic = Clinic.builder().uuid("clinic-v").name("Branch").status(ClinicStatus.VERIFIED).owner(staff)
				.build();
		clinic.setId(30L);
		DoctorProfile doctor = DoctorProfile.builder().uuid("doc-1").status(DoctorStatus.VERIFIED).build();
		doctor.setId(5L);
		when(clinicDao.findByUuid("clinic-v")).thenReturn(clinic);
		when(userDao.userByEmail("staff@example.com")).thenReturn(staff);
		return new Fixture(clinic, doctor);
	}

	private static DoctorAvailability closedToday(DoctorProfile doctor) {
		return closedOn(doctor, LocalDate.now());
	}

	private static DoctorAvailability closedOn(DoctorProfile doctor, LocalDate day) {
		String exceptions = "[{\"date\":\"" + day + "\",\"type\":\"unavailable\",\"title\":\"Off\"}]";
		return DoctorAvailability.builder()
				.doctor(doctor)
				.weeklyScheduleJson(
						"[{\"dayOfWeek\":1,\"startTime\":\"09:00\",\"endTime\":\"17:00\",\"isActive\":true}]")
				.exceptionsJson(exceptions)
				.slotDurationMinutes(30)
				.build();
	}

	private static WalkInCreateRequest emptyWalkIn() {
		return new WalkInCreateRequest(null, null, null, null, null, null);
	}

	private static WalkInCreateRequest walkInWithDoctor() {
		return new WalkInCreateRequest(null, null, null, null, null, "doc-1");
	}

	private record Fixture(Clinic clinic, DoctorProfile doctor) {
	}
}
