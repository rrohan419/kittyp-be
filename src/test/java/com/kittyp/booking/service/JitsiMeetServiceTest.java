package com.kittyp.booking.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import com.kittyp.booking.entity.Booking;
import com.kittyp.booking.enums.BookingMode;

class JitsiMeetServiceTest {

	@Test
	void ensureVideoRoom_rewritesJoinUrlToConfiguredHost() {
		JitsiMeetService service = new JitsiMeetService();
		ReflectionTestUtils.setField(service, "baseUrl", "https://meet.element.io");
		Booking booking = Booking.builder()
				.uuid("b-1")
				.mode(BookingMode.VIDEO)
				.jitsiRoomId("kittypold")
				.videoJoinUrl("https://meet.jit.si/kittypold")
				.build();

		service.ensureVideoRoom(booking);

		assertEquals("kittypold", booking.getJitsiRoomId());
		assertEquals("https://meet.element.io/kittypold", booking.getVideoJoinUrl());
		assertEquals("meet.element.io", service.domain());
	}

	@Test
	void ensureVideoRoom_createsRoomWhenMissing() {
		JitsiMeetService service = new JitsiMeetService();
		ReflectionTestUtils.setField(service, "baseUrl", "https://meet.element.io");
		Booking booking = Booking.builder().uuid("AbC-99").mode(BookingMode.VIDEO).build();

		service.ensureVideoRoom(booking);

		assertTrue(booking.getJitsiRoomId().startsWith("kittypabc99"));
		assertTrue(booking.getVideoJoinUrl().startsWith("https://meet.element.io/"));
	}
}
