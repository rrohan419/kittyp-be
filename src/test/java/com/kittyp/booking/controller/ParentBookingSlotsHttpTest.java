package com.kittyp.booking.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import com.kittyp.booking.service.BookingVideoService;
import com.kittyp.common.dto.ApiResponse;
import com.kittyp.common.exception.GlobalExceptionHandler;
import com.kittyp.common.exception.ResourceNotFoundException;
import com.kittyp.visit.service.VisitService;

class ParentBookingSlotsHttpTest {

	private MockMvc mockMvc;
	private VisitService visitService;

	@BeforeEach
	void setUp() {
		visitService = Mockito.mock(VisitService.class);
		ApiResponse<?> responseBuilder = new ApiResponse<>();
		ParentBookingController controller = new ParentBookingController(visitService,
				Mockito.mock(BookingVideoService.class), responseBuilder);
		mockMvc = MockMvcBuilders.standaloneSetup(controller)
				.setControllerAdvice(new GlobalExceptionHandler(responseBuilder))
				.setMessageConverters(new MappingJackson2HttpMessageConverter())
				.build();
		when(visitService.listDoctorSlots(eq("c1"), eq("d1"), any()))
				.thenReturn(List.of(LocalDateTime.of(2026, 10, 27, 9, 0)));
	}

	@Test
	void publicAliases_ok() throws Exception {
		for (String path : List.of("/api/v1/public/clinics/c1/doctors/d1/slots",
				"/api/v1/public/clinic/c1/doctors/d1/slots", "/api/v1/clinics/c1/doctors/d1/slots",
				"/api/v1/clinic/c1/doctors/d1/slots")) {
			mockMvc.perform(get(path).param("date", "2026-10-27")).andExpect(status().isOk())
					.andExpect(jsonPath("$.data[0]").value("2026-10-27T09:00"));
		}
		mockMvc.perform(get("/api/v1/doctors/d1/slots").param("clinicUuid", "c1").param("date", "2026-10-27"))
				.andExpect(status().isOk());
	}

	@Test
	void badDate_400_missingClinicUuid_400() throws Exception {
		mockMvc.perform(get("/api/v1/public/clinics/c1/doctors/d1/slots").param("date", "bad"))
				.andExpect(status().isBadRequest());
		mockMvc.perform(get("/api/v1/doctors/d1/slots").param("date", "2026-10-27"))
				.andExpect(status().isBadRequest());
	}

	@Test
	void unknown_404_offDay_empty() throws Exception {
		when(visitService.listDoctorSlots(eq("nope"), eq("d1"), any()))
				.thenThrow(new ResourceNotFoundException("clinic", "uuid", "nope"));
		mockMvc.perform(get("/api/v1/public/clinics/nope/doctors/d1/slots").param("date", "2026-10-27"))
				.andExpect(status().isNotFound());

		when(visitService.listDoctorSlots(eq("c1"), eq("d1"), eq(LocalDate.of(2026, 10, 28)))).thenReturn(List.of());
		mockMvc.perform(get("/api/v1/public/clinics/c1/doctors/d1/slots").param("date", "2026-10-28"))
				.andExpect(status().isOk()).andExpect(jsonPath("$.data").isEmpty());
	}
}
