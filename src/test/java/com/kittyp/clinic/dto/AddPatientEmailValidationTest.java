package com.kittyp.clinic.dto;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.kittyp.clinic.dto.ClinicDtos.AddPatientRequest;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;

/** P0-04: RFC-valid client emails must pass server validation. */
class AddPatientEmailValidationTest {

	private Validator validator;

	@BeforeEach
	void setUp() {
		ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
		validator = factory.getValidator();
	}

	@Test
	void acceptsCommonGmail() {
		assertTrue(validator.validate(patient("walkin2@gmail.com")).isEmpty());
	}

	@Test
	void acceptsExampleDomain() {
		assertTrue(validator.validate(patient("sta.walkin2@example.com")).isEmpty());
	}

	@Test
	void rejectsBlankEmail() {
		Set<ConstraintViolation<AddPatientRequest>> violations = validator.validate(patient(" "));
		assertTrue(violations.stream().anyMatch(v -> "ownerEmail".equals(v.getPropertyPath().toString())));
	}

	@Test
	void rejectsClearlyInvalid() {
		Set<ConstraintViolation<AddPatientRequest>> violations = validator.validate(patient("not-an-email"));
		assertTrue(violations.stream().anyMatch(v -> "ownerEmail".equals(v.getPropertyPath().toString())));
	}

	private static AddPatientRequest patient(String email) {
		return new AddPatientRequest("Ada", "Lovelace", email, "9876543210", null, null, null, "Pokey", "Cat", null,
				null, null, null, null, null);
	}
}
