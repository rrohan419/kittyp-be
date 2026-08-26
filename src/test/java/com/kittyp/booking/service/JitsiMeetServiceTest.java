package com.kittyp.booking.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import com.kittyp.booking.entity.Booking;
import com.kittyp.booking.enums.BookingMode;

class JitsiMeetServiceTest {

	private JitsiMeetService service;

	@BeforeEach
	void setUp() {
		service = new JitsiMeetService();
		ReflectionTestUtils.setField(service, "baseUrl", "https://meet.jit.si");
	}

	@Test
	void skipsInPersonBookings() {
		Booking booking = Booking.builder().uuid("abc123").mode(BookingMode.IN_PERSON).build();
		service.ensureVideoRoom(booking);
		assertNull(booking.getJitsiRoomId());
		assertNull(booking.getVideoJoinUrl());
	}

	@Test
	void assignsUniqueRoomForVideo() {
		Booking booking = Booking.builder().uuid("abc123").mode(BookingMode.VIDEO).build();
		service.ensureVideoRoom(booking);
		assertNotNull(booking.getJitsiRoomId());
		assertTrue(booking.getJitsiRoomId().startsWith("kittypabc123"));
		assertEquals("https://meet.jit.si/" + booking.getJitsiRoomId(), booking.getVideoJoinUrl());
		assertEquals("meet.jit.si", service.domain());
	}

	@Test
	void doesNotReplaceExistingRoom() {
		Booking booking = Booking.builder()
				.uuid("abc123")
				.mode(BookingMode.VIDEO)
				.jitsiRoomId("kittyp-existing")
				.videoJoinUrl("https://meet.jit.si/kittyp-existing")
				.build();
		service.ensureVideoRoom(booking);
		assertEquals("kittyp-existing", booking.getJitsiRoomId());
	}
}
