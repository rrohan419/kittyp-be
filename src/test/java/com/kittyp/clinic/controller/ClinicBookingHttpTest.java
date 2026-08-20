package com.kittyp.clinic.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
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

import com.kittyp.clinic.service.ClinicService;
import com.kittyp.common.dto.ApiResponse;
import com.kittyp.common.exception.GlobalExceptionHandler;
import com.kittyp.visit.service.VisitService;

class ClinicBookingHttpTest {

	private VisitService visitService;
	private MockMvc mockMvc;

	@BeforeEach
	void setUp() {
		ClinicService clinicService = Mockito.mock(ClinicService.class);
		visitService = Mockito.mock(VisitService.class);
		ApiResponse<?> responseBuilder = new ApiResponse<>();
		ClinicController controller = new ClinicController(clinicService, visitService, responseBuilder);
		GlobalExceptionHandler advice = new GlobalExceptionHandler(responseBuilder);

		mockMvc = MockMvcBuilders.standaloneSetup(controller)
				.setControllerAdvice(advice)
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
	void patchBooking_forwardsReschedule() throws Exception {
		when(visitService.updateScheduledBooking(eq("clinic-1"), eq("book-1"), any(), eq("clinic@test.com")))
				.thenReturn(null);

		mockMvc.perform(patch("/api/v1/clinic/clinic-1/bookings/book-1")
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"notes\":\"moved\"}"))
				.andExpect(status().isOk());

		verify(visitService).updateScheduledBooking(eq("clinic-1"), eq("book-1"), any(), eq("clinic@test.com"));
	}

	@Test
	void patchBooking_forwardsCancel() throws Exception {
		when(visitService.updateScheduledBooking(eq("clinic-1"), eq("book-1"), any(), eq("clinic@test.com")))
				.thenReturn(null);

		mockMvc.perform(patch("/api/v1/clinic/clinic-1/bookings/book-1")
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"status\":\"CANCELLED\"}"))
				.andExpect(status().isOk());

		verify(visitService).updateScheduledBooking(eq("clinic-1"), eq("book-1"), any(), eq("clinic@test.com"));
	}
}
