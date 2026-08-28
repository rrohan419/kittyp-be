package com.kittyp.auth.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.springframework.http.MediaType;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;

import com.kittyp.auth.service.AuthService;
import com.kittyp.common.dto.ApiResponse;
import com.kittyp.common.dto.PublicSignupRequestDto;
import com.kittyp.common.dto.SignupClinicRequestDto;
import com.kittyp.common.dto.SignupDoctorRequestDto;
import com.kittyp.common.enums.SignupRole;
import com.kittyp.common.exception.GlobalExceptionHandler;
import com.kittyp.common.model.MessageResponse;
import com.kittyp.user.service.UserService;

class AuthSignupHttpTest {

	private AuthService authService;
	private MockMvc mockMvc;

	@BeforeEach
	void setUp() {
		authService = Mockito.mock(AuthService.class);
		UserService userService = Mockito.mock(UserService.class);
		ApiResponse<?> responseBuilder = new ApiResponse<>();
		AuthController controller = new AuthController(authService, responseBuilder, userService);
		GlobalExceptionHandler advice = new GlobalExceptionHandler(responseBuilder);

		LocalValidatorFactoryBean validator = new LocalValidatorFactoryBean();
		validator.afterPropertiesSet();

		mockMvc = MockMvcBuilders.standaloneSetup(controller)
				.setControllerAdvice(advice)
				.setValidator(validator)
				.setMessageConverters(new MappingJackson2HttpMessageConverter())
				.build();

		when(authService.register(any())).thenReturn(new MessageResponse("ok"));
		when(authService.registerDoctor(any())).thenReturn(new MessageResponse("ok"));
		when(authService.registerClinic(any())).thenReturn(new MessageResponse("ok"));
	}

	@ParameterizedTest
	@ValueSource(strings = { "ROLE_ADMIN", "ADMIN", "ROLE_MODERATOR", "ROLE_CLINIC_STAFF", "ROLE_USER", "role_user",
			"STAFF", "SUPERUSER" })
	void signup_rejectsNonAllowlistedRole(String role) throws Exception {
		mockMvc.perform(post("/api/v1/auth/signup")
				.contentType(MediaType.APPLICATION_JSON)
				.content(bodyWithRole(role)))
				.andExpect(status().isBadRequest());

		verify(authService, never()).register(any());
		verify(authService, never()).registerUser(any());
		verify(authService, never()).registerDoctor(any());
		verify(authService, never()).registerClinic(any());
	}

	@Test
	void signup_userRole_dispatchesToRegister() throws Exception {
		mockMvc.perform(post("/api/v1/auth/signup")
				.contentType(MediaType.APPLICATION_JSON)
				.content(bodyWithRole("USER")))
				.andExpect(status().isOk());

		ArgumentCaptor<PublicSignupRequestDto> captor = ArgumentCaptor.forClass(PublicSignupRequestDto.class);
		verify(authService).register(captor.capture());
		assertEquals(SignupRole.USER, captor.getValue().getRole());
		verify(authService, never()).registerDoctor(any());
		verify(authService, never()).registerClinic(any());
	}

	@Test
	void signup_omittedRole_defaultsToUser() throws Exception {
		mockMvc.perform(post("/api/v1/auth/signup")
				.contentType(MediaType.APPLICATION_JSON)
				.content(baseBody()))
				.andExpect(status().isOk());

		ArgumentCaptor<PublicSignupRequestDto> captor = ArgumentCaptor.forClass(PublicSignupRequestDto.class);
		verify(authService).register(captor.capture());
		assertEquals(SignupRole.USER, captor.getValue().getRole());
	}

	@Test
	void signup_doctorAndClinicRoles_dispatchThroughUnifiedRegister() throws Exception {
		mockMvc.perform(post("/api/v1/auth/signup")
				.contentType(MediaType.APPLICATION_JSON)
				.content(bodyWithRole("DOCTOR")))
				.andExpect(status().isOk());
		mockMvc.perform(post("/api/v1/auth/signup")
				.contentType(MediaType.APPLICATION_JSON)
				.content(clinicBody("CLINIC")))
				.andExpect(status().isOk());

		ArgumentCaptor<PublicSignupRequestDto> captor = ArgumentCaptor.forClass(PublicSignupRequestDto.class);
		verify(authService, Mockito.times(2)).register(captor.capture());
		assertEquals(SignupRole.DOCTOR, captor.getAllValues().get(0).getRole());
		assertEquals(SignupRole.CLINIC, captor.getAllValues().get(1).getRole());
	}

	@Test
	void signup_legacyRolesArray_doesNotChangeAllowlistedRole() throws Exception {
		String json = "{\"firstName\":\"Ada\",\"email\":\"ada@example.com\",\"password\":\"Passw0rd!\","
				+ "\"role\":\"USER\",\"roles\":[\"ROLE_ADMIN\",\"ROLE_MODERATOR\"]}";

		mockMvc.perform(post("/api/v1/auth/signup")
				.contentType(MediaType.APPLICATION_JSON)
				.content(json))
				.andExpect(status().isOk());

		ArgumentCaptor<PublicSignupRequestDto> captor = ArgumentCaptor.forClass(PublicSignupRequestDto.class);
		verify(authService).register(captor.capture());
		assertEquals(SignupRole.USER, captor.getValue().getRole());
		assertNotEquals("ROLE_ADMIN", String.valueOf(captor.getValue().getRole()));
	}

	@Test
	void signup_weakPassword_rejectedByValidation() throws Exception {
		String json = "{\"firstName\":\"Ada\",\"email\":\"ada@example.com\",\"password\":\"password\",\"role\":\"USER\"}";
		mockMvc.perform(post("/api/v1/auth/signup")
				.contentType(MediaType.APPLICATION_JSON)
				.content(json))
				.andExpect(status().isBadRequest());
		verify(authService, never()).register(any());
	}

	@Test
	void signup_missingFirstName_rejectedByValidation() throws Exception {
		String json = "{\"email\":\"ada@example.com\",\"password\":\"Passw0rd!\",\"role\":\"USER\"}";
		mockMvc.perform(post("/api/v1/auth/signup")
				.contentType(MediaType.APPLICATION_JSON)
				.content(json))
				.andExpect(status().isBadRequest());
		verify(authService, never()).register(any());
	}

	@Test
	void signup_malformedJson_isBadRequest() throws Exception {
		mockMvc.perform(post("/api/v1/auth/signup")
				.contentType(MediaType.APPLICATION_JSON)
				.content("{not-json"))
				.andExpect(status().isBadRequest());
		verify(authService, never()).register(any());
	}

	@Test
	void signup_unauthenticatedCaller_isAllowed() throws Exception {
		mockMvc.perform(post("/api/v1/auth/signup")
				.contentType(MediaType.APPLICATION_JSON)
				.content(bodyWithRole("USER")))
				.andExpect(status().isOk());
	}

	@Test
	void dedicatedDoctorAndClinicAliases_stillReachMatchingServiceMethods() throws Exception {
		String doctor = "{\"firstName\":\"Ada\",\"email\":\"doc@example.com\",\"password\":\"Passw0rd!\","
				+ "\"registrationNumber\":\"VET-1\",\"degreeCertificateUrl\":\"https://x/d.pdf\","
				+ "\"registrationCertificateUrl\":\"https://x/r.pdf\"}";
		String clinic = "{\"firstName\":\"Ada\",\"email\":\"clinic@example.com\",\"password\":\"Passw0rd!\","
				+ "\"clinicName\":\"Paws Clinic\"}";

		mockMvc.perform(post("/api/v1/auth/signup/doctor")
				.contentType(MediaType.APPLICATION_JSON)
				.content(doctor))
				.andExpect(status().isOk());
		mockMvc.perform(post("/api/v1/auth/signup/clinic")
				.contentType(MediaType.APPLICATION_JSON)
				.content(clinic))
				.andExpect(status().isOk());

		verify(authService).registerDoctor(any(SignupDoctorRequestDto.class));
		verify(authService).registerClinic(any(SignupClinicRequestDto.class));
		verify(authService, never()).register(any());
	}

	private static String bodyWithRole(String role) {
		return "{\"firstName\":\"Ada\",\"email\":\"ada@example.com\",\"password\":\"Passw0rd!\",\"role\":\"" + role
				+ "\"}";
	}

	private static String clinicBody(String role) {
		return "{\"firstName\":\"Ada\",\"email\":\"ada@example.com\",\"password\":\"Passw0rd!\",\"role\":\"" + role
				+ "\",\"clinicName\":\"Paws Clinic\"}";
	}

	private static String baseBody() {
		return "{\"firstName\":\"Ada\",\"email\":\"ada@example.com\",\"password\":\"Passw0rd!\"}";
	}
}
