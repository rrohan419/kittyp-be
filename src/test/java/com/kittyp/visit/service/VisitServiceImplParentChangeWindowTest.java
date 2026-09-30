package com.kittyp.visit.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.function.Function;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.test.util.ReflectionTestUtils;

import com.kittyp.booking.entity.Booking;
import com.kittyp.booking.enums.BookingStatus;
import com.kittyp.booking.repository.BookingRepository;
import com.kittyp.booking.repository.DoctorAvailabilityRepository;
import com.kittyp.booking.service.JitsiMeetService;
import com.kittyp.clinic.dao.ClinicDao;
import com.kittyp.clinic.entity.Clinic;
import com.kittyp.clinic.enums.ClinicStatus;
import com.kittyp.clinic.repository.ClinicDoctorRepository;
import com.kittyp.common.exception.CustomException;
import com.kittyp.doctor.dao.DoctorProfileDao;
import com.kittyp.doctor.entity.DoctorProfile;
import com.kittyp.email.service.ZeptoMailService;
import com.kittyp.user.dao.UserDao;
import com.kittyp.user.entity.Pet;
import com.kittyp.user.entity.User;
import com.kittyp.user.repository.PetsRepository;
import com.kittyp.visit.dto.VisitDtos.ParentBookingPatchRequest;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class VisitServiceImplParentChangeWindowTest {

	private static final String EMAIL = "parent@test.com";
	private static final String BOOKING_UUID = "book-1";
	private static final LocalDateTime NOW = LocalDateTime.of(2026, 10, 1, 10, 0);
	private static final LocalDateTime NEW_SLOT = LocalDateTime.of(2026, 10, 3, 11, 0);

	@Mock
	private UserDao userDao;
	@Mock
	private ClinicDao clinicDao;
	@Mock
	private ClinicDoctorRepository clinicDoctorRepository;
	@Mock
	private DoctorProfileDao doctorProfileDao;
	@Mock
	private BookingRepository bookingRepository;
	@Mock
	private DoctorAvailabilityRepository doctorAvailabilityRepository;
	@Mock
	private JitsiMeetService jitsiMeetService;
	@Mock
	private ZeptoMailService zeptoMailService;
	@Mock
	private PetsRepository petsRepository;
	@InjectMocks
	private VisitServiceImpl visitService;

	private User parent;
	private Booking booking;

	@BeforeEach
	void setUp() {
		ReflectionTestUtils.setField(visitService, "clinicClock", (Function<String, LocalDateTime>) zone -> NOW);
		parent = User.builder().email(EMAIL).uuid("user-1").firstName("Ada").build();
		parent.setId(1L);
		Pet pet = Pet.builder().uuid("pet-1").name("Miso").build();
		parent.setPets(java.util.List.of(pet));
		Clinic clinic = Clinic.builder().uuid("clinic-1").name("Branch").status(ClinicStatus.VERIFIED)
				.timezone("Asia/Kolkata").phone("9999999999").build();
		clinic.setId(10L);
		clinic.setIsActive(true);
		DoctorProfile doctor = DoctorProfile.builder().uuid("doc-1").build();
		doctor.setId(5L);
		booking = Booking.builder()
				.uuid(BOOKING_UUID)
				.pet(pet)
				.owner(parent)
				.clinic(clinic)
				.doctor(doctor)
				.status(BookingStatus.CONFIRMED)
				.timezone("Asia/Kolkata")
				.slotStart(NOW.plusHours(24))
				.build();
		booking.setIsActive(true);
		when(userDao.userByEmail(EMAIL)).thenReturn(parent);
		when(clinicDao.findOwnerUserId(10L)).thenReturn(null);
		when(clinicDao.findByOwnerUserId(1L)).thenReturn(null);
		when(bookingRepository.findByUuid(BOOKING_UUID)).thenReturn(Optional.of(booking));
		when(bookingRepository.save(any(Booking.class))).thenAnswer(inv -> inv.getArgument(0));
		when(doctorAvailabilityRepository.findByDoctor_Id(5L)).thenReturn(Optional.empty());
		when(bookingRepository.findOverlappingForDoctor(any(), any(), any(), any())).thenReturn(java.util.List.of());
	}

	@Test
	void parentReschedulesTwentyFourHoursBefore() {
		reschedule();
		assertEquals(NEW_SLOT, booking.getSlotStart());
		verify(zeptoMailService, times(1)).sendAppointmentRescheduledEmail(any(), any(), any(), any(), any(), any(),
				eq(BOOKING_UUID), any(), any(), any(), any(), any());
	}

	@Test
	void parentReschedulesExactlySixHoursBefore() {
		booking.setSlotStart(NOW.plusHours(6));
		reschedule();
		assertEquals(NEW_SLOT, booking.getSlotStart());
	}

	@Test
	void parentRescheduleInsideWindowIsRejectedAndSendsNoEmail() {
		booking.setSlotStart(NOW.plusHours(6).minusMinutes(1));
		assertThrows(CustomException.class, this::reschedule);
		assertEquals(NOW.plusHours(6).minusMinutes(1), booking.getSlotStart());
		verify(zeptoMailService, never()).sendAppointmentRescheduledEmail(any(), any(), any(), any(), any(), any(),
				any(), any(), any(), any(), any(), any());
	}

	@Test
	void parentCancelsTwentyFourHoursBefore() {
		cancel();
		assertEquals(BookingStatus.CANCELLED, booking.getStatus());
		verify(zeptoMailService, times(1)).sendAppointmentCancelledEmail(any(), any(), any(), any(), any(), any(),
				eq(BOOKING_UUID), any(), any(), any());
	}

	@Test
	void parentCancelsExactlySixHoursBefore() {
		booking.setSlotStart(NOW.plusHours(6));
		cancel();
		assertEquals(BookingStatus.CANCELLED, booking.getStatus());
	}

	@Test
	void parentCancelInsideWindowIsRejected() {
		booking.setSlotStart(NOW.plusHours(6).minusMinutes(1));
		assertThrows(CustomException.class, this::cancel);
		assertEquals(BookingStatus.CONFIRMED, booking.getStatus());
		verify(zeptoMailService, never()).sendAppointmentCancelledEmail(any(), any(), any(), any(), any(), any(),
				any(), any(), any(), any());
	}

	@Test
	void otherParentIsRejected() {
		User other = User.builder().email("other@test.com").uuid("user-2").build();
		other.setId(2L);
		other.setPets(java.util.List.of());
		when(userDao.userByEmail("other@test.com")).thenReturn(other);
		when(petsRepository.findOptionalByUuid("pet-1")).thenReturn(Optional.empty());
		assertThrows(AccessDeniedException.class, () -> visitService.updateMyParentBooking(BOOKING_UUID,
				new ParentBookingPatchRequest(null, null, BookingStatus.CANCELLED), "other@test.com"));
		assertEquals(BookingStatus.CONFIRMED, booking.getStatus());
	}

	@Test
	void secondCancelIsRejected() {
		booking.setStatus(BookingStatus.CANCELLED);
		assertThrows(CustomException.class, this::cancel);
		verify(zeptoMailService, never()).sendAppointmentCancelledEmail(any(), any(), any(), any(), any(), any(),
				any(), any(), any(), any());
	}

	@Test
	void completedBookingIsRejected() {
		booking.setStatus(BookingStatus.COMPLETED);
		assertThrows(CustomException.class, this::reschedule);
		assertEquals(BookingStatus.COMPLETED, booking.getStatus());
		verify(zeptoMailService, never()).sendAppointmentRescheduledEmail(any(), any(), any(), any(), any(), any(),
				any(), any(), any(), any(), any(), any());
	}

	private void reschedule() {
		visitService.updateMyParentBooking(BOOKING_UUID, new ParentBookingPatchRequest(NEW_SLOT, null, null), EMAIL);
	}

	private void cancel() {
		visitService.updateMyParentBooking(BOOKING_UUID,
				new ParentBookingPatchRequest(null, null, BookingStatus.CANCELLED), EMAIL);
	}
}
