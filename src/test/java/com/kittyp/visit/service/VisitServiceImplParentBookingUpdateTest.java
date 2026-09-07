package com.kittyp.visit.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
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

import com.kittyp.booking.entity.Booking;
import com.kittyp.booking.enums.BookingStatus;
import com.kittyp.booking.repository.BookingRepository;
import com.kittyp.booking.service.JitsiMeetService;
import com.kittyp.clinic.entity.Clinic;
import com.kittyp.clinic.enums.ClinicStatus;
import com.kittyp.common.exception.CustomException;
import com.kittyp.user.dao.UserDao;
import com.kittyp.user.entity.Pet;
import com.kittyp.user.entity.User;
import com.kittyp.visit.dto.VisitDtos.ParentBookingPatchRequest;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class VisitServiceImplParentBookingUpdateTest {

	private static final String EMAIL = "parent@test.com";
	private static final String BOOKING_UUID = "book-parent-1";
	private static final String PET_UUID = "pet-1";

	@Mock
	private UserDao userDao;
	@Mock
	private BookingRepository bookingRepository;
	@Mock
	private JitsiMeetService jitsiMeetService;

	@InjectMocks
	private VisitServiceImpl visitService;

	private User parent;
	private Pet pet;
	private Booking booking;

	@BeforeEach
	void setUp() {
		parent = User.builder().email(EMAIL).uuid("user-1").build();
		parent.setId(1L);
		pet = Pet.builder().uuid(PET_UUID).name("Pokey").build();
		parent.setPets(java.util.List.of(pet));

		Clinic clinic = Clinic.builder().uuid("clinic-1").status(ClinicStatus.VERIFIED).timezone("Asia/Kolkata")
				.build();
		clinic.setId(10L);
		clinic.setIsActive(true);

		booking = Booking.builder()
				.uuid(BOOKING_UUID)
				.pet(pet)
				.owner(parent)
				.clinic(clinic)
				.status(BookingStatus.CONFIRMED)
				.slotStart(LocalDateTime.now().plusDays(1).withHour(10).withMinute(0).withSecond(0).withNano(0))
				.build();
		booking.setIsActive(true);

		when(userDao.userByEmail(EMAIL)).thenReturn(parent);
		when(bookingRepository.findByUuid(BOOKING_UUID)).thenReturn(Optional.of(booking));
		when(bookingRepository.save(any(Booking.class))).thenAnswer(inv -> inv.getArgument(0));
	}

	@Test
	void parentCanCancelUpcomingBooking() {
		Booking result = null;
		var model = visitService.updateMyParentBooking(BOOKING_UUID,
				new ParentBookingPatchRequest(null, null, BookingStatus.CANCELLED), EMAIL);
		assertEquals(BookingStatus.CANCELLED, booking.getStatus());
		assertEquals(BOOKING_UUID, model.uuid());
		verify(bookingRepository).save(booking);
	}

	@Test
	void parentCannotMarkNoShow() {
		CustomException ex = assertThrows(CustomException.class,
				() -> visitService.updateMyParentBooking(BOOKING_UUID,
						new ParentBookingPatchRequest(null, null, BookingStatus.NO_SHOW), EMAIL));
		assertEquals(HttpStatus.BAD_REQUEST, ex.getHttpStatus());
		assertEquals("Parents can only cancel upcoming appointments", ex.getMessage());
	}
}
