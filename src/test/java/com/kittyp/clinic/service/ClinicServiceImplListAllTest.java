package com.kittyp.clinic.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
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
import com.kittyp.user.entity.User;

@ExtendWith(MockitoExtension.class)
class ClinicServiceImplListAllTest {

	@Mock
	private ClinicDao clinicDao;

	@Mock
	private ClinicDoctorRepository clinicDoctorRepository;

	@InjectMocks
	private ClinicServiceImpl clinicService;

	@Test
	void listAllClinics_nullStatus_defaultsPending() {
		Clinic clinic = Clinic.builder().uuid("c1").name("Alpha").owner(null).build();
		clinic.setId(1L);
		when(clinicDao.findAllFetchOwner()).thenReturn(List.of(clinic));

		List<ClinicModel> list = assertDoesNotThrow(() -> clinicService.listAllClinics());

		assertEquals(1, list.size());
		assertEquals("PENDING", list.get(0).status());
		assertEquals("c1", list.get(0).uuid());
	}

	@Test
	void listAllClinics_ownerLookupFailure_stillReturnsClinic() {
		User owner = User.builder().email("owner@example.com").password("x").uuid("u1").build();
		owner.setId(9L);
		Clinic clinic = Clinic.builder().uuid("c2").name("Beta").status(ClinicStatus.PENDING).owner(owner).build();
		clinic.setId(2L);
		when(clinicDao.findAllFetchOwner()).thenReturn(List.of(clinic));
		when(clinicDoctorRepository.existsByClinic_IdAndDoctor_User_IdAndIsActiveTrue(2L, 9L))
				.thenThrow(new IllegalStateException("affiliation query failed"));

		List<ClinicModel> list = assertDoesNotThrow(() -> clinicService.listAllClinics());

		assertEquals(1, list.size());
		assertEquals("PENDING", list.get(0).status());
		assertEquals("c2", list.get(0).uuid());
	}
}
