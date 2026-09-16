package com.kittyp.booking.dto;

public record VideoCallRingPayload(
		String bookingUuid,
		String joinPath,
		String callerName,
		String petName,
		String callerPhotoUrl) {
}
