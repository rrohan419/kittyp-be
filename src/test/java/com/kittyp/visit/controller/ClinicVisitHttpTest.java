package com.kittyp.visit.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.http.MediaType;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;

import com.kittyp.common.dto.ApiResponse;
import com.kittyp.common.exception.GlobalExceptionHandler;
import com.kittyp.visit.service.VisitService;

class ClinicVisitHttpTest {

	private VisitService visitService;
	private MockMvc mockMvc;

	@BeforeEach
	void setUp() {
		visitService = Mockito.mock(VisitService.class);
		ApiResponse<?> responseBuilder = new ApiResponse<>();
		ClinicVisitController controller = new ClinicVisitController(visitService, responseBuilder);
		GlobalExceptionHandler advice = new GlobalExceptionHandler(responseBuilder);

		LocalValidatorFactoryBean validator = new LocalValidatorFactoryBean();
		validator.afterPropertiesSet();

		mockMvc = MockMvcBuilders.standaloneSetup(controller)
				.setControllerAdvice(advice)
				.setValidator(validator)
				.setMessageConverters(new MappingJackson2HttpMessageConverter())
				.build();

		SecurityContextHolder.getContext().setAuthentication(
				new UsernamePasswordAuthenticationToken("clinic@test.com", "n", List.of()));
	}

	@AfterEach
	void tearDown() {
		SecurityContextHolder.clearContext();
	}

	@Test
	void walkIn_postsToMappedEndpoint() throws Exception {
		when(visitService.createWalkIn(eq("clinic-1"), any(), eq("clinic@test.com"))).thenReturn(null);

		mockMvc.perform(post("/api/v1/clinic/clinic-1/visits/walk-in")
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"petUuid\":\"pet-1\"}"))
				.andExpect(status().isCreated());

		verify(visitService).createWalkIn(eq("clinic-1"), any(), eq("clinic@test.com"));
	}

	@Test
	void visits_listsBoard() throws Exception {
		when(visitService.listClinicVisits(eq("clinic-1"), isNull(), isNull(), isNull(), eq("clinic@test.com")))
				.thenReturn(List.of());

		mockMvc.perform(get("/api/v1/clinic/clinic-1/visits"))
				.andExpect(status().isOk());

		verify(visitService).listClinicVisits(eq("clinic-1"), isNull(), isNull(), isNull(), eq("clinic@test.com"));
	}

	@Test
	void patchVisit_forwardsStatusChange() throws Exception {
		when(visitService.patchVisit(eq("clinic-1"), eq("visit-1"), any(), eq("clinic@test.com"))).thenReturn(null);

		mockMvc.perform(patch("/api/v1/clinic/clinic-1/visits/visit-1")
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"status\":\"CHECKED_IN\"}"))
				.andExpect(status().isOk());

		verify(visitService).patchVisit(eq("clinic-1"), eq("visit-1"), any(), eq("clinic@test.com"));
	}
}
