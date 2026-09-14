package com.kittyp.auth.controller;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
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
import com.kittyp.auth.dto.MasterTotpEnrollmentModel;
import com.kittyp.auth.service.MasterTotpService;
import com.kittyp.common.dto.ApiResponse;
import com.kittyp.common.exception.GlobalExceptionHandler;

@SpringJUnitWebConfig(AdminMasterTotpControllerTest.TestConfig.class)
class AdminMasterTotpControllerTest {

	@Autowired
	private WebApplicationContext wac;

	@Autowired
	private MasterTotpService masterTotpService;

	private MockMvc mockMvc;

	@BeforeEach
	void setUp() {
		Mockito.reset(masterTotpService);
		when(masterTotpService.enrollment()).thenReturn(new MasterTotpEnrollmentModel(
				true,
				"otpauth://totp/Kittyp:master?secret=JBSWY3DPEHPK3PXP&issuer=Kittyp&period=30&digits=6",
				"Kittyp",
				"master",
				30,
				6));
		mockMvc = MockMvcBuilders.webAppContextSetup(wac).apply(springSecurity()).build();
	}

	@Test
	void enrollment_admin_ok() throws Exception {
		mockMvc.perform(get("/api/v1/admin/master-totp")
				.with(user("admin").authorities(new SimpleGrantedAuthority("ROLE_ADMIN"))))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data.enabled").value(true))
				.andExpect(jsonPath("$.data.otpauthUri").value(org.hamcrest.Matchers.startsWith("otpauth://totp/")));
		verify(masterTotpService).enrollment();
	}

	@ParameterizedTest
	@ValueSource(strings = { "ROLE_USER", "ROLE_DOCTOR", "ROLE_CLINIC_ADMIN", "ROLE_MODERATOR" })
	void enrollment_nonAdmin_forbidden(String authority) throws Exception {
		mockMvc.perform(get("/api/v1/admin/master-totp")
				.with(user("tester").authorities(new SimpleGrantedAuthority(authority))))
				.andExpect(status().isForbidden());
	}

	@Configuration
	@EnableWebMvc
	@EnableWebSecurity
	@EnableMethodSecurity(prePostEnabled = true)
	static class TestConfig implements WebMvcConfigurer {

		@Bean
		MasterTotpService masterTotpService() {
			return Mockito.mock(MasterTotpService.class);
		}

		@Bean
		ApiResponse<?> apiResponse() {
			return new ApiResponse<>();
		}

		@Bean
		AdminMasterTotpController adminMasterTotpController(MasterTotpService masterTotpService,
				ApiResponse<?> apiResponse) {
			return new AdminMasterTotpController(masterTotpService, apiResponse);
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
