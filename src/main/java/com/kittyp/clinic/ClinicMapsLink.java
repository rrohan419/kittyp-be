package com.kittyp.clinic;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

import com.kittyp.clinic.entity.Clinic;

/**
 * Google Maps search link for a clinic. Coordinates win; otherwise the street address and city.
 */
public final class ClinicMapsLink {

	private static final String MAPS_SEARCH = "https://www.google.com/maps/search/?api=1&query=";

	private ClinicMapsLink() {
	}

	public static String url(Clinic clinic) {
		if (clinic == null) {
			return "";
		}
		return url(clinic.getLatitude(), clinic.getLongitude(), clinic.getAddress(), clinic.getCity());
	}

	public static String url(Double latitude, Double longitude, String street, String city) {
		if (latitude != null && longitude != null && Double.isFinite(latitude) && Double.isFinite(longitude)) {
			return MAPS_SEARCH + latitude + "," + longitude;
		}
		String address = place(street, city);
		if (address.isEmpty()) {
			return "";
		}
		return MAPS_SEARCH + URLEncoder.encode(address, StandardCharsets.UTF_8);
	}

	public static String place(String street, String city) {
		String line = street == null ? "" : street.trim();
		String town = city == null ? "" : city.trim();
		if (line.isEmpty()) {
			return town;
		}
		if (town.isEmpty()) {
			return line;
		}
		return line + ", " + town;
	}
}
