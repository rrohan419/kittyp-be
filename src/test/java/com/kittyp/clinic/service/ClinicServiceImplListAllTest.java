package com.kittyp.clinic.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.kittyp.clinic.dao.ClinicDao;
import com.kittyp.clinic.dto.ClinicDtos.ClinicModel;
import com.kittyp.clinic.entity.Clinic;
import com.kittyp.clinic.enums.ClinicStatus;
import com.kittyp.clinic.repository.ClinicDoctorRepository;
import com.kittyp.doctor.dao.DoctorProfileDao;
import com.kittyp.email.service.ZeptoMailService;
import com.kittyp.user.entity.User;

@ExtendWith(MockitoExtension.class)
class ClinicServiceImplListAllTest {

	@Mock
	private ClinicDao clinicDao;

	@Mock
	private ClinicDoctorRepository clinicDoctorRepository;

	@Mock
	private DoctorProfileDao doctorProfileDao;

	@Mock
	private ZeptoMailService zeptoMailService;

	@InjectMocks
	private ClinicServiceImpl clinicService;

	@Test
	void listAllClinics_nullStatus_defaultsPending() {
		Clinic clinic = Clinic.builder().uuid("c1").name("Alpha").owner(null).build();
		clinic.setId(1L);
		when(clinicDao.findAllOrganizationFetchOwner()).thenReturn(List.of(clinic));

		List<ClinicModel> list = assertDoesNotThrow(() -> clinicService.listAllClinics());

		assertEquals(1, list.size());
		assertEquals("PENDING", list.get(0).status());
		assertEquals("c1", list.get(0).uuid());
	}

	@Test
	void listAllClinics_verifiedStatus_isVerified() {
		Clinic clinic = Clinic.builder().uuid("c3").name("Gamma").status(ClinicStatus.VERIFIED).owner(null).build();
		clinic.setId(3L);
		when(clinicDao.findAllOrganizationFetchOwner()).thenReturn(List.of(clinic));

		List<ClinicModel> list = clinicService.listAllClinics();

		assertEquals(1, list.size());
		assertEquals("VERIFIED", list.get(0).status());
	}

	@Test
	void updateStatusForAdmin_verified_returnsVerified() {
		Clinic clinic = Clinic.builder().uuid("c1").name("Alpha").status(ClinicStatus.PENDING)
				.email("admin@kittyp.test").owner(null).build();
		clinic.setId(1L);
		when(clinicDao.findByUuid("c1")).thenReturn(clinic);
		when(clinicDao.saveClinic(clinic)).thenAnswer(invocation -> invocation.getArgument(0));

		ClinicModel model = clinicService.updateStatusForAdmin("c1", ClinicStatus.VERIFIED);

		assertEquals("VERIFIED", model.status());
		assertEquals(ClinicStatus.VERIFIED, clinic.getStatus());
		verify(zeptoMailService).sendClinicProfileVerified(
				eq("admin@kittyp.test"), eq("there"), eq("Alpha"), eq("http://localhost:8080/clinic"));
	}

	@Test
	void updateStatusForAdmin_rejected_doesNotMail() {
		Clinic clinic = Clinic.builder().uuid("c1").name("Alpha").status(ClinicStatus.PENDING)
				.email("admin@kittyp.test").owner(null).build();
		clinic.setId(1L);
		when(clinicDao.findByUuid("c1")).thenReturn(clinic);
		when(clinicDao.saveClinic(clinic)).thenAnswer(invocation -> invocation.getArgument(0));

		clinicService.updateStatusForAdmin("c1", ClinicStatus.REJECTED);

		verify(zeptoMailService, never()).sendClinicProfileVerified(any(), any(), any(), any());
	}

	@Test
	void updateStatusForAdmin_alreadyVerified_doesNotMailAgain() {
		Clinic clinic = Clinic.builder().uuid("c1").name("Alpha").status(ClinicStatus.VERIFIED)
				.email("admin@kittyp.test").owner(null).build();
		clinic.setId(1L);
		when(clinicDao.findByUuid("c1")).thenReturn(clinic);
		when(clinicDao.saveClinic(clinic)).thenAnswer(invocation -> invocation.getArgument(0));

		clinicService.updateStatusForAdmin("c1", ClinicStatus.VERIFIED);

		verify(zeptoMailService, never()).sendClinicProfileVerified(any(), any(), any(), any());
	}

	@Test
	void listAllClinics_ownerLookupFailure_stillReturnsClinic() {
		User owner = User.builder().email("owner@example.com").password("x").uuid("u1").build();
		owner.setId(9L);
		Clinic clinic = Clinic.builder().uuid("c2").name("Beta").status(ClinicStatus.PENDING).owner(owner).build();
		clinic.setId(2L);
		when(clinicDao.findAllOrganizationFetchOwner()).thenReturn(List.of(clinic));
		when(clinicDoctorRepository.existsByClinic_IdAndDoctor_User_IdAndIsActiveTrue(2L, 9L))
				.thenReturn(false);

		List<ClinicModel> list = assertDoesNotThrow(() -> clinicService.listAllClinics());

		assertEquals(1, list.size());
		assertEquals("PENDING", list.get(0).status());
		assertEquals("c2", list.get(0).uuid());
	}
}
