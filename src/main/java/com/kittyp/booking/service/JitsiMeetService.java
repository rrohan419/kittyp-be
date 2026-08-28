package com.kittyp.booking.service;

import java.net.URI;
import java.util.Locale;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.kittyp.booking.entity.Booking;
import com.kittyp.booking.enums.BookingMode;

@Service
public class JitsiMeetService {

	@Value("${app.jitsi.base-url:https://meet.jit.si}")
	private String baseUrl;

	public void ensureVideoRoom(Booking booking) {
		if (booking == null || booking.getMode() != BookingMode.VIDEO) {
			return;
		}
		if (hasRoom(booking)) {
			return;
		}
		String token = booking.getUuid() == null ? "" : booking.getUuid().replaceAll("[^A-Za-z0-9]", "");
		String salt = UUID.randomUUID().toString().replace("-", "").substring(0, 10);
		String room = ("kittyp" + token + salt).toLowerCase(Locale.ROOT);
		booking.setJitsiRoomId(room);
		booking.setVideoJoinUrl(normalizedBase() + "/" + room);
	}

	public String domain() {
		try {
			String host = URI.create(normalizedBase()).getHost();
			return host == null || host.isBlank() ? "meet.jit.si" : host;
		} catch (IllegalArgumentException ex) {
			return "meet.jit.si";
		}
	}

	private static boolean hasRoom(Booking booking) {
		return booking.getJitsiRoomId() != null && !booking.getJitsiRoomId().isBlank()
				&& booking.getVideoJoinUrl() != null && !booking.getVideoJoinUrl().isBlank();
	}

	private String normalizedBase() {
		String raw = baseUrl == null || baseUrl.isBlank() ? "https://meet.jit.si" : baseUrl.trim();
		while (raw.endsWith("/")) {
			raw = raw.substring(0, raw.length() - 1);
		}
		return raw;
	}
}
