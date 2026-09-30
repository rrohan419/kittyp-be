package com.kittyp.clinic;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

import com.kittyp.clinic.entity.Clinic;

class ClinicMapsLinkTest {

	@Test
	void usesCoordinatesWhenBothPresent() {
		Clinic clinic = Clinic.builder().address("12 Park Road").city("Pune").latitude(18.52).longitude(73.85).build();

		assertEquals("https://www.google.com/maps/search/?api=1&query=18.52,73.85", ClinicMapsLink.url(clinic));
	}

	@Test
	void usesEncodedAddressWhenCoordinatesMissing() {
		Clinic clinic = Clinic.builder().address("12 Park Road").city("Pune").build();

		assertEquals("https://www.google.com/maps/search/?api=1&query=12+Park+Road%2C+Pune", ClinicMapsLink.url(clinic));
	}

	@Test
	void emptyWhenNoLocation() {
		assertEquals("", ClinicMapsLink.url(null));
		assertEquals("", ClinicMapsLink.url(Clinic.builder().name("Clinic").build()));
	}
}
