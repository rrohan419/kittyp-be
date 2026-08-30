package com.kittyp.common.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;
import java.util.Map;

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
import com.kittyp.common.dto.ApiResponse;
import com.kittyp.common.exception.GlobalExceptionHandler;
import com.kittyp.common.model.HealthOptimizeResponse;
import com.kittyp.common.model.HealthOptimizeTarget;
import com.kittyp.common.model.SystemHealthResponse;
import com.kittyp.common.service.AdminService;
import com.kittyp.common.service.HealthOptimizeService;
import com.kittyp.common.service.SystemHealthService;

@SpringJUnitWebConfig(AdminHealthOptimizeAuthTest.TestConfig.class)
class AdminHealthOptimizeAuthTest {

	@Autowired
	private WebApplicationContext wac;

	@Autowired
	private HealthOptimizeService healthOptimizeService;

	@Autowired
	private SystemHealthService systemHealthService;

	private MockMvc mockMvc;

	@BeforeEach
	void setUp() {
		Mockito.reset(healthOptimizeService, systemHealthService);
		when(systemHealthService.snapshot()).thenReturn(new SystemHealthResponse("UP", Map.of()));
		when(healthOptimizeService.optimize(any())).thenReturn(new HealthOptimizeResponse("MEMORY", true, "ok", "",
				new SystemHealthResponse("UP", Map.of()), new SystemHealthResponse("UP", Map.of())));
		mockMvc = MockMvcBuilders.webAppContextSetup(wac).apply(springSecurity()).build();
	}

	@Test
	void optimize_admin_ok() throws Exception {
		mockMvc.perform(post("/api/v1/admin/system-health/optimize")
				.with(user("admin").authorities(new SimpleGrantedAuthority("ROLE_ADMIN")))
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"target\":\"MEMORY\"}"))
				.andExpect(status().isOk());
		verify(healthOptimizeService).optimize(HealthOptimizeTarget.MEMORY);
	}

	@ParameterizedTest
	@ValueSource(strings = { "ROLE_USER", "ROLE_DOCTOR", "ROLE_CLINIC_ADMIN" })
	void optimize_nonAdmin_forbidden(String authority) throws Exception {
		mockMvc.perform(post("/api/v1/admin/system-health/optimize")
				.with(user("tester").authorities(new SimpleGrantedAuthority(authority)))
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"target\":\"DISK\"}"))
				.andExpect(status().isForbidden());
		verify(healthOptimizeService, never()).optimize(any());
	}

	@Test
	void snapshot_admin_ok() throws Exception {
		mockMvc.perform(get("/api/v1/admin/system-health")
				.with(user("admin").authorities(new SimpleGrantedAuthority("ROLE_ADMIN"))))
				.andExpect(status().isOk());
		verify(systemHealthService).snapshot();
	}

	@Test
	void loadStart_admin_disabled_notFound() throws Exception {
		mockMvc.perform(post("/api/v1/admin/system-health/load-test/start")
				.with(user("admin").authorities(new SimpleGrantedAuthority("ROLE_ADMIN")))
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"target\":\"MEMORY\"}"))
				.andExpect(status().isNotFound());
	}

	@ParameterizedTest
	@ValueSource(strings = { "ROLE_USER", "ROLE_DOCTOR", "ROLE_CLINIC_ADMIN" })
	void loadStart_nonAdmin_forbidden(String authority) throws Exception {
		mockMvc.perform(post("/api/v1/admin/system-health/load-test/start")
				.with(user("tester").authorities(new SimpleGrantedAuthority(authority)))
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"target\":\"WORKERS\"}"))
				.andExpect(status().isForbidden());
	}

	@Configuration
	@EnableWebMvc
	@EnableWebSecurity
	@EnableMethodSecurity(prePostEnabled = true)
	static class TestConfig implements WebMvcConfigurer {

		@Bean
		HealthOptimizeService healthOptimizeService() {
			return Mockito.mock(HealthOptimizeService.class);
		}

		@Bean
		SystemHealthService systemHealthService() {
			return Mockito.mock(SystemHealthService.class);
		}

		@Bean
		AdminService adminService() {
			return Mockito.mock(AdminService.class);
		}

		@Bean
		ApiResponse<?> apiResponse() {
			return new ApiResponse<>();
		}

		@Bean
		AdminDashboardController adminDashboardController(ApiResponse<?> apiResponse, AdminService adminService,
				SystemHealthService systemHealthService, HealthOptimizeService healthOptimizeService) {
			@SuppressWarnings("unchecked")
			org.springframework.beans.factory.ObjectProvider<com.kittyp.common.service.HealthLoadTestService> loadTest = Mockito
					.mock(org.springframework.beans.factory.ObjectProvider.class);
			return new AdminDashboardController(apiResponse, adminService, systemHealthService, healthOptimizeService,
					loadTest);
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
