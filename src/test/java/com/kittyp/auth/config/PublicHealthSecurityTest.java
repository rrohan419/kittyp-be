package com.kittyp.auth.config;

import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.test.context.junit.jupiter.web.SpringJUnitWebConfig;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.kittyp.auth.util.AuthEntryPointJwt;
import com.kittyp.common.controller.PublicHealthController;

@SpringJUnitWebConfig(PublicHealthSecurityTest.TestConfig.class)
class PublicHealthSecurityTest {

	@Autowired
	private WebApplicationContext wac;

	private MockMvc mockMvc;

	@BeforeEach
	void setUp() {
		mockMvc = MockMvcBuilders.webAppContextSetup(wac).apply(springSecurity()).build();
	}

	@Test
	void anonymousHealthOkStatusOnly() throws Exception {
		mockMvc.perform(get("/health"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.status").value("UP"))
				.andExpect(jsonPath("$.hikariPool").doesNotExist())
				.andExpect(jsonPath("$.memory").doesNotExist());
	}

	@Test
	void anonymousActuatorHealthOkStatusOnly() throws Exception {
		mockMvc.perform(get("/actuator/health"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.status").value("UP"))
				.andExpect(jsonPath("$.components").doesNotExist());
	}

	@Test
	void anonymousActuatorInfoUnauthorized() throws Exception {
		mockMvc.perform(get("/actuator/info"))
				.andExpect(status().isUnauthorized());
	}

	@Configuration
	@EnableWebMvc
	@EnableWebSecurity
	static class TestConfig implements WebMvcConfigurer {

		@Bean
		ObjectMapper objectMapper() {
			ObjectMapper mapper = new ObjectMapper();
			mapper.registerModule(new JavaTimeModule());
			return mapper;
		}

		@Bean
		AuthEntryPointJwt authEntryPointJwt(ObjectMapper objectMapper) {
			return new AuthEntryPointJwt(objectMapper);
		}

		@Bean
		SecurityFilterChain filterChain(HttpSecurity http, AuthEntryPointJwt authEntryPointJwt) throws Exception {
			return http.csrf(AbstractHttpConfigurer::disable)
					.exceptionHandling(ex -> ex.authenticationEntryPoint(authEntryPointJwt))
					.authorizeHttpRequests(auth -> auth
							.requestMatchers("/health", "/actuator/health").permitAll()
							.anyRequest().authenticated())
					.build();
		}

		@Bean
		PublicHealthController publicHealthController() {
			return new PublicHealthController(List.<HealthIndicator>of());
		}

		@Bean
		InfoStubController infoStubController() {
			return new InfoStubController();
		}

		@Override
		public void configureMessageConverters(List<HttpMessageConverter<?>> converters) {
			converters.add(new MappingJackson2HttpMessageConverter(new ObjectMapper()));
		}
	}

	@RestController
	static class InfoStubController {
		@GetMapping("/actuator/info")
		public Map<String, String> info() {
			return Map.of("app", "kittyp");
		}
	}
}
