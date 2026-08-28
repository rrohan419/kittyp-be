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

import com.kittyp.clinic.dto.ClinicDtos.ClinicPetListModel;
import com.kittyp.clinic.service.ClinicService;
import com.kittyp.common.dto.ApiResponse;
import com.kittyp.common.exception.GlobalExceptionHandler;
import com.kittyp.visit.service.VisitService;

class ClinicPetHttpTest {

	private ClinicService clinicService;
	private MockMvc mockMvc;

	@BeforeEach
	void setUp() {
		clinicService = Mockito.mock(ClinicService.class);
		VisitService visitService = Mockito.mock(VisitService.class);
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
	void patchPet_forwardsUpdate() throws Exception {
		when(clinicService.updatePet(eq("clinic-1"), eq("pet-1"), any(), eq("clinic@test.com")))
				.thenReturn(null);

		mockMvc.perform(patch("/api/v1/clinic/clinic-1/pets/pet-1")
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"name\":\"Milo\",\"species\":\"Cat\",\"weight\":\"4 kg\"}"))
				.andExpect(status().isOk());

		verify(clinicService).updatePet(eq("clinic-1"), eq("pet-1"), any(), eq("clinic@test.com"));
	}

	@Test
	void patchPet_returnsUpdatedModel() throws Exception {
		ClinicPetListModel model = new ClinicPetListModel("pet-1", "pet-1", "Milo", "Cat", "Siamese",
				"Female", null, "4 kg", "CHIP-1", null, null, "owner-1", "Owner", null, null, false, null);
		when(clinicService.updatePet(eq("clinic-1"), eq("pet-1"), any(), eq("clinic@test.com")))
				.thenReturn(model);

		mockMvc.perform(patch("/api/v1/clinic/clinic-1/pets/pet-1")
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"name\":\"Milo\"}"))
				.andExpect(status().isOk());

		verify(clinicService).updatePet(eq("clinic-1"), eq("pet-1"), any(), eq("clinic@test.com"));
	}
}
