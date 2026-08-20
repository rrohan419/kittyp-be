package com.kittyp.visit.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

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
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;

import com.kittyp.booking.entity.Booking;
import com.kittyp.booking.enums.BookingMode;
import com.kittyp.booking.enums.BookingStatus;
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
import com.kittyp.user.dao.UserDao;
import com.kittyp.user.entity.User;
import com.kittyp.visit.dto.VisitDtos.ScheduleBookingPatchRequest;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class VisitServiceImplUpdateScheduledBookingTest {

	private static final String CLINIC_UUID = "clinic-1";
	private static final String BOOKING_UUID = "book-1";
	private static final String EMAIL = "clinic@test.com";
	private static final String DOCTOR_UUID = "doc-1";

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
	@Mock
	private BookingRepository bookingRepository;
	@Mock
	private DoctorAvailabilityRepository doctorAvailabilityRepository;

	@InjectMocks
	private VisitServiceImpl visitService;

	private User owner;
	private Clinic clinic;
	private DoctorProfile doctor;
	private Booking booking;

	@BeforeEach
	void setUp() {
		owner = User.builder().email(EMAIL).password("x").uuid("u-1").firstName("Clinic").lastName("Admin").build();
		owner.setId(1L);
		clinic = Clinic.builder().uuid(CLINIC_UUID).name("Branch").status(ClinicStatus.VERIFIED).owner(owner).build();
		clinic.setId(100L);
		clinic.setIsActive(true);
		doctor = DoctorProfile.builder().uuid(DOCTOR_UUID).user(owner).build();
		doctor.setId(5L);
		LocalDateTime start = futureSlot(10, 0);
		booking = Booking.builder()
				.uuid(BOOKING_UUID)
				.clinic(clinic)
				.doctor(doctor)
				.slotStart(start)
				.slotEnd(start.plusMinutes(30))
				.mode(BookingMode.IN_PERSON)
				.status(BookingStatus.CONFIRMED)
				.notes("old")
				.build();
		booking.setId(10L);
		booking.setIsActive(true);

		when(clinicDao.findByUuid(CLINIC_UUID)).thenReturn(clinic);
		when(userDao.userByEmail(EMAIL)).thenReturn(owner);
		when(bookingRepository.findByUuid(BOOKING_UUID)).thenReturn(Optional.of(booking));
		when(bookingRepository.save(any(Booking.class))).thenAnswer(inv -> inv.getArgument(0));
	}

	@Test
	void cancelConfirmedBooking() {
		ScheduleBookingPatchRequest request = new ScheduleBookingPatchRequest(null, null, null,
				BookingStatus.CANCELLED, null);

		assertEquals(BookingStatus.CANCELLED,
				visitService.updateScheduledBooking(CLINIC_UUID, BOOKING_UUID, request, EMAIL).status());
	}

	@Test
	void markNoShow() {
		assertEquals(BookingStatus.NO_SHOW,
				visitService.updateScheduledBooking(CLINIC_UUID, BOOKING_UUID,
						new ScheduleBookingPatchRequest(null, null, null, BookingStatus.NO_SHOW, null), EMAIL)
						.status());
	}

	@Test
	void staffCanEdit() {
		User staffUser = User.builder().email("staff@test.com").password("x").uuid("u-2").build();
		staffUser.setId(2L);
		when(userDao.userByEmail("staff@test.com")).thenReturn(staffUser);
		when(clinicStaffDao.isActiveMember(100L, 2L)).thenReturn(true);
		when(clinicDoctorRepository.existsByClinic_IdAndDoctor_User_IdAndIsActiveTrue(100L, 2L)).thenReturn(false);

		assertEquals("desk note", visitService.updateScheduledBooking(CLINIC_UUID, BOOKING_UUID,
				new ScheduleBookingPatchRequest(null, null, "desk note", null, null), "staff@test.com").notes());
	}

	@Test
	void notesOnlyDoesNotRecheckHours() {
		ScheduleBookingPatchRequest request = new ScheduleBookingPatchRequest(null, null, "front desk note", null,
				null);

		assertEquals("front desk note",
				visitService.updateScheduledBooking(CLINIC_UUID, BOOKING_UUID, request, EMAIL).notes());
		verify(doctorAvailabilityRepository, never()).findByDoctor_Id(any());
	}

	@Test
	void refuseCompletedBooking() {
		booking.setStatus(BookingStatus.COMPLETED);
		ScheduleBookingPatchRequest request = new ScheduleBookingPatchRequest(null, null, "x", null, null);

		CustomException ex = assertThrows(CustomException.class,
				() -> visitService.updateScheduledBooking(CLINIC_UUID, BOOKING_UUID, request, EMAIL));
		assertEquals(HttpStatus.BAD_REQUEST, ex.getHttpStatus());
		assertEquals("This appointment can no longer be edited", ex.getMessage());
	}

	@Test
	void refuseEmptyPatch() {
		CustomException ex = assertThrows(CustomException.class, () -> visitService.updateScheduledBooking(
				CLINIC_UUID, BOOKING_UUID, new ScheduleBookingPatchRequest(null, null, null, null, null), EMAIL));
		assertEquals("No changes provided", ex.getMessage());
	}

	@Test
	void refuseStatusCompletedViaPatch() {
		CustomException ex = assertThrows(CustomException.class,
				() -> visitService.updateScheduledBooking(CLINIC_UUID, BOOKING_UUID,
						new ScheduleBookingPatchRequest(null, null, null, BookingStatus.COMPLETED, null), EMAIL));
		assertEquals("Clinic can only cancel or mark no-show from this screen", ex.getMessage());
	}

	@Test
	void shutdownClinicIsReadOnly() {
		clinic.setStatus(ClinicStatus.SHUTDOWN);
		CustomException ex = assertThrows(CustomException.class, () -> visitService.updateScheduledBooking(
				CLINIC_UUID, BOOKING_UUID, new ScheduleBookingPatchRequest(null, null, "x", null, null), EMAIL));
		assertEquals("This clinic is shut down and is read-only.", ex.getMessage());
	}

	@Test
	void wrongClinicIsNotFound() {
		Clinic other = Clinic.builder().uuid("other").name("Other").status(ClinicStatus.VERIFIED).owner(owner).build();
		other.setId(200L);
		booking.setClinic(other);

		assertThrows(ResourceNotFoundException.class, () -> visitService.updateScheduledBooking(
				CLINIC_UUID, BOOKING_UUID, new ScheduleBookingPatchRequest(null, null, "x", null, null), EMAIL));
	}

	@Test
	void inactiveBookingIsNotFound() {
		booking.setIsActive(false);
		when(bookingRepository.findByUuid(BOOKING_UUID)).thenReturn(Optional.of(booking));

		assertThrows(ResourceNotFoundException.class, () -> visitService.updateScheduledBooking(
				CLINIC_UUID, BOOKING_UUID, new ScheduleBookingPatchRequest(null, null, "x", null, null), EMAIL));
	}

	@Test
	void overlapExcludesSelf() {
		stubDoctorHoursAndAffiliation();
		when(bookingRepository.findOverlappingForDoctor(eq(5L), any(), any(), any())).thenReturn(List.of(booking));
		LocalDateTime next = futureSlot(11, 0);

		assertEquals(next, visitService.updateScheduledBooking(CLINIC_UUID, BOOKING_UUID,
				new ScheduleBookingPatchRequest(null, next, null, null, null), EMAIL).slotStart());
	}

	@Test
	void overlapWithOtherBookingConflicts() {
		stubDoctorHoursAndAffiliation();
		Booking other = Booking.builder().uuid("other").clinic(clinic).doctor(doctor).status(BookingStatus.CONFIRMED)
				.slotStart(futureSlot(11, 0)).slotEnd(futureSlot(11, 30)).mode(BookingMode.IN_PERSON).build();
		other.setId(99L);
		when(bookingRepository.findOverlappingForDoctor(eq(5L), any(), any(), any())).thenReturn(List.of(other));

		CustomException ex = assertThrows(CustomException.class,
				() -> visitService.updateScheduledBooking(CLINIC_UUID, BOOKING_UUID,
						new ScheduleBookingPatchRequest(null, futureSlot(11, 0), null, null, null), EMAIL));
		assertEquals(HttpStatus.CONFLICT, ex.getHttpStatus());
	}

	@Test
	void outsideHoursRejected() {
		stubDoctorHoursAndAffiliation();
		CustomException ex = assertThrows(CustomException.class,
				() -> visitService.updateScheduledBooking(CLINIC_UUID, BOOKING_UUID,
						new ScheduleBookingPatchRequest(null, futureSlot(21, 0), null, null, null), EMAIL));
		assertEquals("Doctor is not available at this time", ex.getMessage());
	}

	@Test
	void pastSlotRejected() {
		stubDoctorHoursAndAffiliation();
		CustomException ex = assertThrows(CustomException.class,
				() -> visitService.updateScheduledBooking(CLINIC_UUID, BOOKING_UUID,
						new ScheduleBookingPatchRequest(null, LocalDateTime.now().minusHours(1), null, null, null),
						EMAIL));
		assertEquals("Cannot book a slot in the past", ex.getMessage());
	}

	@Test
	void snapsToHalfHour() {
		stubDoctorHoursAndAffiliation();
		when(bookingRepository.findOverlappingForDoctor(eq(5L), any(), any(), any())).thenReturn(List.of());
		LocalDateTime raw = futureSlot(10, 17);

		assertEquals(futureSlot(10, 30), visitService.updateScheduledBooking(CLINIC_UUID, BOOKING_UUID,
				new ScheduleBookingPatchRequest(null, raw, null, null, null), EMAIL).slotStart());
	}

	@Test
	void inactiveDoctorAtClinicRejected() {
		when(clinicDoctorRepository.findByClinic_IdAndDoctor_Uuid(100L, "doc-2")).thenReturn(Optional.empty());
		CustomException ex = assertThrows(CustomException.class,
				() -> visitService.updateScheduledBooking(CLINIC_UUID, BOOKING_UUID,
						new ScheduleBookingPatchRequest("doc-2", null, null, null, null), EMAIL));
		assertEquals("Doctor is not active at this clinic", ex.getMessage());
	}

	@Test
	void affiliatedDoctorCannotEditSomeoneElsesBooking() {
		User otherDoctorUser = User.builder().email("doc@test.com").password("x").uuid("u-9").build();
		otherDoctorUser.setId(9L);
		when(userDao.userByEmail("doc@test.com")).thenReturn(otherDoctorUser);
		when(clinicDao.findByUuid(CLINIC_UUID)).thenReturn(clinic);
		when(clinicStaffDao.isActiveMember(100L, 9L)).thenReturn(false);
		when(clinicDoctorRepository.existsByClinic_IdAndDoctor_User_IdAndIsActiveTrue(100L, 9L)).thenReturn(true);
		DoctorProfile other = DoctorProfile.builder().uuid("doc-other").user(otherDoctorUser).build();
		other.setId(8L);
		when(doctorProfileDao.findByUserId(9L)).thenReturn(other);

		assertThrows(AccessDeniedException.class, () -> visitService.updateScheduledBooking(CLINIC_UUID, BOOKING_UUID,
				new ScheduleBookingPatchRequest(null, null, "nope", null, null), "doc@test.com"));
	}

	private void stubDoctorHoursAndAffiliation() {
		when(doctorAvailabilityRepository.findByDoctor_Id(5L)).thenReturn(Optional.empty());
		ClinicDoctor affiliation = ClinicDoctor.builder().clinic(clinic).doctor(doctor).isActive(true).build();
		when(clinicDoctorRepository.findByClinic_IdAndDoctor_Uuid(100L, DOCTOR_UUID))
				.thenReturn(Optional.of(affiliation));
	}

	private static LocalDateTime futureSlot(int hour, int minute) {
		return LocalDateTime.now().plusDays(2).withHour(hour).withMinute(minute).withSecond(0).withNano(0);
	}
}
