package com.kittyp.booking.dto;

public record IncomingVideoCallModel(
		String bookingUuid,
		String title,
		String body,
		String joinPath,
		String callerName,
		String callerPhotoUrl) {
}
