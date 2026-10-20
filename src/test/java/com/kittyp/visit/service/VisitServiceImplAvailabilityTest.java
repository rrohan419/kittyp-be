package com.kittyp.visit.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
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

import com.kittyp.booking.entity.Booking;
import com.kittyp.booking.entity.DoctorAvailability;
import com.kittyp.booking.repository.BookingRepository;
import com.kittyp.booking.repository.DoctorAvailabilityRepository;
import com.kittyp.clinic.dao.ClinicDao;
import com.kittyp.clinic.dao.ClinicStaffDao;
import com.kittyp.clinic.entity.Clinic;
import com.kittyp.clinic.entity.ClinicDoctor;
import com.kittyp.clinic.enums.ClinicStatus;
import com.kittyp.clinic.repository.ClinicDoctorRepository;
import com.kittyp.common.exception.CustomException;
import com.kittyp.common.exception.ResourceNotFoundException;
import com.kittyp.doctor.dao.DoctorProfileDao;
import com.kittyp.doctor.entity.DoctorProfile;
import com.kittyp.doctor.enums.DoctorStatus;
import com.kittyp.user.dao.UserDao;
import com.kittyp.user.entity.User;
import com.kittyp.user.service.PetAccessGuard;
import com.kittyp.visit.dto.VisitDtos.ParentBookingCreateRequest;
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

	@Mock
	private BookingRepository bookingRepository;

	@Mock
	private PetAccessGuard petAccessGuard;

	@InjectMocks
	private VisitServiceImpl visitService;

	@Test
	void createWalkIn_assignedDoctor_doesNotCheckOnlineAvailability() {
		stubVerifiedClinicWithDoctor();

		CustomException ex = assertThrows(CustomException.class,
				() -> visitService.createWalkIn("clinic-v", walkInWithDoctor(), "staff@example.com"));
		assertEquals("Provide petUuid or owner + newPet for walk-in", ex.getMessage());
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

	@Test
	void listDoctorSlots_scheduledDay_returnsSlotsExceptBooked() {
		Fixture f = stubVerifiedClinicWithDoctor();
		LocalDate day = LocalDate.now().plusDays(7);
		when(doctorAvailabilityRepository.findByDoctor_Id(5L)).thenReturn(Optional.of(openOn(f.doctor, day)));
		when(bookingRepository.findOverlappingForDoctor(eq(5L), any(), any(), any()))
				.thenReturn(List.of(Booking.builder().slotStart(day.atTime(9, 0)).slotEnd(day.atTime(9, 30)).build()));

		List<LocalDateTime> slots = visitService.listDoctorSlots("clinic-v", "doc-1", day);

		assertEquals(List.of(day.atTime(9, 30), day.atTime(10, 0), day.atTime(10, 30), day.atTime(11, 0),
				day.atTime(11, 30)), slots);
	}

	@Test
	void listDoctorSlots_unknownDoctor_404() {
		Clinic clinic = Clinic.builder().uuid("clinic-v").status(ClinicStatus.VERIFIED).build();
		clinic.setId(30L);
		when(clinicDao.findByUuid("clinic-v")).thenReturn(clinic);

		assertThrows(ResourceNotFoundException.class, () -> visitService.listDoctorSlots("clinic-v", "nope", LocalDate.now()));
	}

	@Test
	void listDoctorSlots_shutdownClinic_400NoLeak() {
		Clinic clinic = Clinic.builder().uuid("clinic-v").status(ClinicStatus.SHUTDOWN).build();
		clinic.setId(30L);
		when(clinicDao.findByUuid("clinic-v")).thenReturn(clinic);

		CustomException ex = assertThrows(CustomException.class,
				() -> visitService.listDoctorSlots("clinic-v", "doc-1", LocalDate.now()));
		assertEquals(400, ex.getHttpStatus().value());
	}

	@Test
	void createParentBooking_unknownDoctor_still400() {
		stubVerifiedClinic();

		CustomException ex = assertThrows(CustomException.class, () -> visitService.createParentBooking(
				new ParentBookingCreateRequest("clinic-v", "nope", "pet-1", LocalDateTime.now().plusDays(1), null, null),
				"staff@example.com"));
		assertEquals(400, ex.getHttpStatus().value());
		assertEquals("Doctor is not active at this clinic", ex.getMessage());
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
		lenient().when(userDao.userByEmail("staff@example.com")).thenReturn(staff);
		return new Fixture(clinic, doctor);
	}

	private static DoctorAvailability openOn(DoctorProfile doctor, LocalDate day) {
		return DoctorAvailability.builder().doctor(doctor)
				.weeklyScheduleJson("[{\"dayOfWeek\":" + day.getDayOfWeek().getValue()
						+ ",\"startTime\":\"09:00\",\"endTime\":\"12:00\",\"isActive\":true}]")
				.slotDurationMinutes(30).build();
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
