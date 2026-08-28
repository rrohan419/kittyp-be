package com.kittyp.common.dto;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.kittyp.common.enums.SignupRole;

class PublicSignupRequestDtoTest {

	private final ObjectMapper objectMapper = new ObjectMapper();

	@Test
	void jackson_rejectsElevatedAndUnknownRoles() {
		assertThrows(JsonProcessingException.class,
				() -> objectMapper.readValue(signupJson("ROLE_ADMIN"), PublicSignupRequestDto.class));
		assertThrows(JsonProcessingException.class,
				() -> objectMapper.readValue(signupJson("ADMIN"), PublicSignupRequestDto.class));
		assertThrows(JsonProcessingException.class,
				() -> objectMapper.readValue(signupJson("ROLE_CLINIC_STAFF"), PublicSignupRequestDto.class));
		assertThrows(JsonProcessingException.class,
				() -> objectMapper.readValue(signupJson("ROLE_MODERATOR"), PublicSignupRequestDto.class));
	}

	@Test
	void jackson_acceptsAllowlistedRoles() throws Exception {
		assertEquals(SignupRole.USER, objectMapper.readValue(signupJson("USER"), PublicSignupRequestDto.class).getRole());
		assertEquals(SignupRole.DOCTOR, objectMapper.readValue(signupJson("DOCTOR"), PublicSignupRequestDto.class).getRole());
		assertEquals(SignupRole.CLINIC, objectMapper.readValue(signupJson("CLINIC"), PublicSignupRequestDto.class).getRole());
	}

	@Test
	void jackson_omittedRole_defaultsToUser() throws Exception {
		PublicSignupRequestDto parsed = objectMapper.readValue(
				"{\"firstName\":\"Ada\",\"email\":\"ada@example.com\",\"password\":\"Passw0rd!\"}",
				PublicSignupRequestDto.class);
		assertEquals(SignupRole.USER, parsed.getRole());
	}

	@Test
	void jackson_copiesDoctorAndClinicFields() throws Exception {
		String json = "{\"firstName\":\"Ada\",\"email\":\"ada@example.com\",\"password\":\"Passw0rd!\","
				+ "\"role\":\"DOCTOR\",\"phoneNumber\":\"9876543210\",\"registrationNumber\":\"VET-1\","
				+ "\"clinicName\":\"Paws\",\"address\":\"1 Main\",\"phone\":\"111\"}";
		PublicSignupRequestDto parsed = objectMapper.readValue(json, PublicSignupRequestDto.class);
		assertEquals("9876543210", parsed.toDoctorRequest().getPhoneNumber());
		assertEquals("VET-1", parsed.toDoctorRequest().getRegistrationNumber());
		assertEquals("Paws", parsed.toClinicRequest().getClinicName());
		assertEquals("1 Main", parsed.toClinicRequest().getAddress());
	}

	private static String signupJson(String role) {
		return "{\"firstName\":\"Ada\",\"email\":\"ada@example.com\",\"password\":\"Passw0rd!\",\"role\":\"" + role
				+ "\"}";
	}
}
