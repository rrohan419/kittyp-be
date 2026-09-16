package com.kittyp.common.geo;

import org.springframework.http.HttpStatus;

import com.kittyp.common.exception.CustomException;

public final class GeoBounds {

	private GeoBounds() {
	}

	public static void validate(Double latitude, Double longitude) {
		if (latitude == null && longitude == null) {
			return;
		}
		if (latitude == null || longitude == null || !Double.isFinite(latitude) || !Double.isFinite(longitude)
				|| latitude < -90 || latitude > 90 || longitude < -180 || longitude > 180) {
			throw new CustomException("Invalid map coordinates", HttpStatus.BAD_REQUEST);
		}
	}
}
