package com.kittyp.clinic.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.test.context.junit.jupiter.web.SpringJUnitWebConfig;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.PropertyAccessor;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.kittyp.clinic.dto.ClinicDtos.ClinicModel;
import com.kittyp.clinic.enums.ClinicStatus;
import com.kittyp.clinic.service.ClinicService;
import com.kittyp.common.dto.ApiResponse;
import com.kittyp.common.exception.GlobalExceptionHandler;

@SpringJUnitWebConfig(AdminClinicControllerAuthTest.TestConfig.class)
class AdminClinicControllerAuthTest {

	private static final ClinicModel SAMPLE = new ClinicModel("CLINIC1", "Happy Paws", "LIC-1", "1 Main St",
			"9999999999", "clinic@test.com", "Asia/Kolkata", null, "VERIFIED");

	@Autowired
	private WebApplicationContext wac;

	@Autowired
	private ClinicService clinicService;

	private MockMvc mockMvc;

	@BeforeEach
	void setUp() {
		Mockito.reset(clinicService);
		when(clinicService.listAllClinics()).thenReturn(List.of(SAMPLE));
		when(clinicService.getByUuidForAdmin(anyString())).thenReturn(SAMPLE);
		when(clinicService.updateStatusForAdmin(anyString(), any())).thenReturn(SAMPLE);
		mockMvc = MockMvcBuilders.webAppContextSetup(wac).apply(springSecurity()).build();
	}

	@Test
	void list_admin_ok() throws Exception {
		mockMvc.perform(get("/api/v1/admin/clinics")
				.with(user("admin").authorities(new SimpleGrantedAuthority("ROLE_ADMIN"))))
				.andExpect(status().isOk());
		verify(clinicService).listAllClinics();
	}

	@Test
	void detail_admin_ok() throws Exception {
		mockMvc.perform(get("/api/v1/admin/clinics/CLINIC1")
				.with(user("admin").authorities(new SimpleGrantedAuthority("ROLE_ADMIN"))))
				.andExpect(status().isOk());
		verify(clinicService).getByUuidForAdmin("CLINIC1");
	}

	@Test
	void list_moderator_ok() throws Exception {
		mockMvc.perform(get("/api/v1/admin/clinics")
				.with(user("mod").authorities(new SimpleGrantedAuthority("ROLE_MODERATOR"))))
				.andExpect(status().isOk());
		verify(clinicService).listAllClinics();
	}

	@ParameterizedTest
	@ValueSource(strings = { "ROLE_USER", "ROLE_DOCTOR", "ROLE_CLINIC_ADMIN", "ROLE_CLINIC_STAFF" })
	void list_nonAdmin_forbidden(String authority) throws Exception {
		mockMvc.perform(get("/api/v1/admin/clinics")
				.with(user("tester").authorities(new SimpleGrantedAuthority(authority))))
				.andExpect(status().isForbidden());
		verify(clinicService, never()).listAllClinics();
	}

	@Test
	void updateStatus_admin_ok() throws Exception {
		mockMvc.perform(patch("/api/v1/admin/clinics/CLINIC1/status")
				.with(user("admin").authorities(new SimpleGrantedAuthority("ROLE_ADMIN")))
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"status\":\"VERIFIED\"}"))
				.andExpect(status().isOk());
		verify(clinicService).updateStatusForAdmin("CLINIC1", ClinicStatus.VERIFIED);
	}

	@Test
	void updateStatus_clinicAdmin_forbidden() throws Exception {
		mockMvc.perform(patch("/api/v1/admin/clinics/CLINIC1/status")
				.with(user("clinic").authorities(new SimpleGrantedAuthority("ROLE_CLINIC_ADMIN")))
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"status\":\"VERIFIED\"}"))
				.andExpect(status().isForbidden());
		verify(clinicService, never()).updateStatusForAdmin(anyString(), eq(ClinicStatus.VERIFIED));
	}

	@Configuration
	@EnableWebMvc
	@EnableWebSecurity
	@EnableMethodSecurity(prePostEnabled = true)
	static class TestConfig implements WebMvcConfigurer {

		@Bean
		ClinicService clinicService() {
			return Mockito.mock(ClinicService.class);
		}

		@Bean
		ApiResponse<?> apiResponse() {
			return new ApiResponse<>();
		}

		@Bean
		AdminClinicController adminClinicController(ApiResponse<?> apiResponse, ClinicService clinicService) {
			return new AdminClinicController(clinicService, apiResponse);
		}

		@Bean
		GlobalExceptionHandler globalExceptionHandler(ApiResponse<?> apiResponse) {
			return new GlobalExceptionHandler(apiResponse);
		}

		@Bean
		SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
			return http.csrf(AbstractHttpConfigurer::disable)
					.authorizeHttpRequests(auth -> auth.anyRequest().authenticated())
					.build();
		}

		@Override
		public void configureMessageConverters(List<HttpMessageConverter<?>> converters) {
			ObjectMapper mapper = new ObjectMapper();
			mapper.registerModule(new JavaTimeModule());
			mapper.setVisibility(PropertyAccessor.FIELD, JsonAutoDetect.Visibility.ANY);
			converters.add(new MappingJackson2HttpMessageConverter(mapper));
		}
	}
}
