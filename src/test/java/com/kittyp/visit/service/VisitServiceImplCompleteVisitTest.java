package com.kittyp.visit.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.kittyp.booking.enums.BookingMode;
import com.kittyp.clinic.entity.Clinic;
import com.kittyp.clinic.repository.ClinicDoctorRepository;
import com.kittyp.doctor.dao.DoctorProfileDao;
import com.kittyp.doctor.entity.DoctorProfile;
import com.kittyp.doctor.repository.DoctorReviewRepository;
import com.kittyp.user.dao.UserDao;
import com.kittyp.user.entity.Pet;
import com.kittyp.user.entity.User;
import com.kittyp.visit.dao.VisitDao;
import com.kittyp.visit.dto.VisitDtos.VisitModel;
import com.kittyp.visit.entity.Visit;
import com.kittyp.visit.enums.VisitSource;
import com.kittyp.visit.enums.VisitStatus;
import com.kittyp.visit.enums.VisitUrgency;

@ExtendWith(MockitoExtension.class)
class VisitServiceImplCompleteVisitTest {

	@Mock
	private VisitDao visitDao;

	@Mock
	private UserDao userDao;

	@Mock
	private DoctorProfileDao doctorProfileDao;

	@Mock
	private ClinicDoctorRepository clinicDoctorRepository;

	@Mock
	private DoctorReviewRepository doctorReviewRepository;

	@InjectMocks
	private VisitServiceImpl visitService;

	@Test
	void completeVisit_inProgress_becomesCheckingOut() {
		User user = User.builder().email("doc@example.com").password("x").uuid("u-doc").firstName("Ajit")
				.lastName("Attarde").build();
		user.setId(8L);
		DoctorProfile profile = DoctorProfile.builder().user(user).build();
		profile.setId(3L);
		profile.setUuid("doc-3");

		Clinic clinic = Clinic.builder().uuid("clinic-1").name("Clinic one").build();
		clinic.setId(10L);
		Pet pet = Pet.builder().uuid("pet-1").name("Hulk").type("Cat").build();

		Visit visit = Visit.builder()
				.uuid("visit-1")
				.clinic(clinic)
				.pet(pet)
				.doctor(profile)
				.source(VisitSource.WALK_IN)
				.channel(BookingMode.IN_PERSON)
				.status(VisitStatus.IN_PROGRESS)
				.urgency(VisitUrgency.URGENT)
				.assessment("Not okay")
				.healthEventUuid("he-1")
				.build();

		when(userDao.userByEmail("doc@example.com")).thenReturn(user);
		when(doctorProfileDao.findByUserId(8L)).thenReturn(profile);
		when(visitDao.findByUuid("visit-1")).thenReturn(Optional.of(visit));
		when(clinicDoctorRepository.existsByClinic_IdAndDoctor_User_IdAndIsActiveTrue(10L, 8L)).thenReturn(true);
		when(visitDao.save(any(Visit.class))).thenAnswer(inv -> inv.getArgument(0));
		when(doctorReviewRepository.findByVisit_Uuid("visit-1")).thenReturn(Optional.empty());

		VisitModel model = visitService.completeVisit("visit-1", "doc@example.com");

		assertEquals(VisitStatus.CHECKING_OUT, visit.getStatus());
		assertNotNull(visit.getCheckingOutAt());
		assertEquals(VisitStatus.CHECKING_OUT, model.status());
	}
}
