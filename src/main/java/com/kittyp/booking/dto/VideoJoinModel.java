package com.kittyp.booking.dto;

public record VideoJoinModel(
		String bookingUuid,
		String roomName,
		String domain,
		String joinUrl,
		String displayName) {
}
