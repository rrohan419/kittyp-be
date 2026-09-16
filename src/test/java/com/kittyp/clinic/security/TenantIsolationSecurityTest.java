package com.kittyp.clinic.security;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.kittyp.auth.filter.TenantSecurityFilter;
import com.kittyp.auth.tenant.TenantAccessService;
import com.kittyp.clinic.controller.ClinicController;
import com.kittyp.clinic.dao.ClinicDao;
import com.kittyp.clinic.dao.ClinicStaffDao;
import com.kittyp.clinic.entity.Clinic;
import com.kittyp.clinic.repository.ClinicDoctorRepository;
import com.kittyp.clinic.service.ClinicService;
import com.kittyp.common.dto.ApiResponse;
import com.kittyp.common.exception.GlobalExceptionHandler;
import com.kittyp.user.dao.UserDao;
import com.kittyp.user.entity.User;
import com.kittyp.visit.controller.DoctorVisitController;
import com.kittyp.visit.service.VisitService;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;

class TenantIsolationSecurityTest {

	private static final String CLINIC_2 = UUID.randomUUID().toString();
	private static final String PET = UUID.randomUUID().toString();
	private static final String VISIT_2 = UUID.randomUUID().toString();
	private static final String ACTOR = "doc@clinic1.test";

	private ListAppender<ILoggingEvent> auditAppender;
	private Logger auditLogger;

	@BeforeEach
	void attachAudit() {
		auditLogger = (Logger) LoggerFactory.getLogger("SECURITY_AUDIT");
		auditAppender = new ListAppender<>();
		auditAppender.start();
		auditLogger.addAppender(auditAppender);
	}

	@AfterEach
	void tearDown() {
		auditLogger.detachAppender(auditAppender);
		SecurityContextHolder.clearContext();
	}

	@Test
	void clinic1Actor_readingClinic2Patient_is403AndAudited() throws Exception {
		ClinicDao clinicDao = Mockito.mock(ClinicDao.class);
		ClinicStaffDao staffDao = Mockito.mock(ClinicStaffDao.class);
		ClinicDoctorRepository doctorRepo = Mockito.mock(ClinicDoctorRepository.class);
		UserDao userDao = Mockito.mock(UserDao.class);
		Clinic clinic2 = Clinic.builder().uuid(CLINIC_2).build();
		clinic2.setId(2L);
		User actor = User.builder().email(ACTOR).password("x").build();
		actor.setId(1L);
		when(clinicDao.findByUuid(CLINIC_2)).thenReturn(clinic2);
		when(userDao.userByEmail(ACTOR)).thenReturn(actor);
		when(clinicDao.findOwnerUserId(2L)).thenReturn(99L);
		when(staffDao.isActiveMember(2L, 1L)).thenReturn(false);
		when(doctorRepo.existsByClinic_IdAndDoctor_User_IdAndIsActiveTrue(2L, 1L)).thenReturn(false);

		ClinicService clinicService = Mockito.mock(ClinicService.class);
		VisitService visitService = Mockito.mock(VisitService.class);
		ApiResponse<?> responseBuilder = new ApiResponse<>();
		ClinicController controller = new ClinicController(clinicService, visitService, responseBuilder);
		TenantAccessService tenantAccess = new TenantAccessService(clinicDao, staffDao, doctorRepo, userDao);
		MockMvc mockMvc = MockMvcBuilders.standaloneSetup(controller)
				.addFilters(new TenantSecurityFilter(tenantAccess, new ObjectMapper()))
				.setControllerAdvice(new GlobalExceptionHandler(responseBuilder))
				.setMessageConverters(new MappingJackson2HttpMessageConverter())
				.build();

		asUser(ACTOR, "ROLE_DOCTOR");

		mockMvc.perform(get("/api/v1/clinic/" + CLINIC_2 + "/patients/" + PET))
				.andExpect(status().isForbidden());

		verify(clinicService, never()).patientDetail(anyString(), anyString(), anyString());
		assertTrue(auditAppender.list.stream().anyMatch(event -> {
			String msg = event.getFormattedMessage();
			return msg.contains("event=TENANT_ISOLATION_DENIED") && msg.contains(CLINIC_2) && msg.contains(ACTOR);
		}), auditAppender.list.toString());
	}

	@Test
	void doctorClinic1_chartingClinic2Visit_is403() throws Exception {
		VisitService visitService = Mockito.mock(VisitService.class);
		ApiResponse<?> responseBuilder = new ApiResponse<>();
		DoctorVisitController controller = new DoctorVisitController(visitService, responseBuilder);
		when(visitService.saveChart(eq(VISIT_2), any(), eq(ACTOR)))
				.thenThrow(new AccessDeniedException("You are not a doctor at this clinic"));
		MockMvc mockMvc = MockMvcBuilders.standaloneSetup(controller)
				.setControllerAdvice(new GlobalExceptionHandler(responseBuilder))
				.setMessageConverters(new MappingJackson2HttpMessageConverter())
				.build();

		asUser(ACTOR, "ROLE_DOCTOR");

		mockMvc.perform(put("/api/v1/doctor/visits/" + VISIT_2 + "/chart")
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"assessment\":\"rx\"}"))
				.andExpect(status().isForbidden());
	}

	@Test
	void attendedPatients_foreignClinicUuid_is403() throws Exception {
		VisitService visitService = Mockito.mock(VisitService.class);
		ApiResponse<?> responseBuilder = new ApiResponse<>();
		DoctorVisitController controller = new DoctorVisitController(visitService, responseBuilder);
		doThrow(new AccessDeniedException("You do not have access to this clinic."))
				.when(visitService).pageMyAttendedPatients(eq(ACTOR), eq(CLINIC_2), any(), any(), any());
		MockMvc mockMvc = MockMvcBuilders.standaloneSetup(controller)
				.setControllerAdvice(new GlobalExceptionHandler(responseBuilder))
				.setMessageConverters(new MappingJackson2HttpMessageConverter())
				.build();

		asUser(ACTOR, "ROLE_DOCTOR");

		mockMvc.perform(get("/api/v1/doctor/patients/attended").param("clinicUuid", CLINIC_2))
				.andExpect(status().isForbidden());
	}

	@Test
	void staffCannotHitDoctorChart_preAuthorizeForbidsNonDoctors() throws Exception {
		org.springframework.security.access.prepost.PreAuthorize pre = DoctorVisitController.class
				.getMethod("chart", String.class,
						com.kittyp.visit.dto.VisitDtos.VisitChartRequest.class)
				.getAnnotation(org.springframework.security.access.prepost.PreAuthorize.class);
		assertEquals(com.kittyp.common.constants.KeyConstant.IS_ROLE_DOCTOR, pre.value());
	}

	private static void asUser(String email, String role) {
		SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(email, "n",
				List.of(new SimpleGrantedAuthority(role))));
	}
}
