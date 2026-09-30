package com.kittyp.visit.service;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import com.kittyp.booking.entity.Booking;
import com.kittyp.booking.enums.BookingStatus;
import com.kittyp.booking.repository.BookingRepository;
import com.kittyp.booking.repository.DoctorAvailabilityRepository;
import com.kittyp.booking.service.JitsiMeetService;
import com.kittyp.clinic.dao.ClinicDao;
import com.kittyp.clinic.dao.ClinicStaffDao;
import com.kittyp.clinic.entity.Clinic;
import com.kittyp.clinic.entity.ClinicDoctor;
import com.kittyp.clinic.entity.ClinicPetOwner;
import com.kittyp.clinic.enums.ClinicStatus;
import com.kittyp.clinic.repository.ClinicDoctorRepository;
import com.kittyp.clinic.service.ClinicOwnerUserLinkService;
import com.kittyp.doctor.dao.DoctorProfileDao;
import com.kittyp.doctor.entity.DoctorProfile;
import com.kittyp.doctor.enums.DoctorStatus;
import com.kittyp.email.service.ZeptoMailService;
import com.kittyp.user.dao.UserDao;
import com.kittyp.user.entity.Pet;
import com.kittyp.user.entity.User;
import com.kittyp.user.repository.PetsRepository;
import com.kittyp.user.service.PetAccessGuard;
import com.kittyp.visit.dto.VisitDtos.ParentBookingCreateRequest;
import com.kittyp.visit.dto.VisitDtos.ScheduleBookingCreateRequest;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class VisitServiceImplAppointmentEmailTest {

	@Mock
	private ClinicDao clinicDao;
	@Mock
	private ClinicStaffDao clinicStaffDao;
	@Mock
	private ClinicDoctorRepository clinicDoctorRepository;
	@Mock
	private DoctorProfileDao doctorProfileDao;
	@Mock
	private UserDao userDao;
	@Mock
	private BookingRepository bookingRepository;
	@Mock
	private DoctorAvailabilityRepository doctorAvailabilityRepository;
	@Mock
	private JitsiMeetService jitsiMeetService;
	@Mock
	private ZeptoMailService zeptoMailService;
	@Mock
	private PetsRepository petsRepository;
	@Mock
	private ClinicOwnerUserLinkService clinicOwnerUserLinkService;
	@Mock
	private ParentBookingEnrollmentService parentBookingEnrollmentService;
	@Mock
	private PetAccessGuard petAccessGuard;

	@InjectMocks
	private VisitServiceImpl visitService;

	@Test
	void clinicScheduleSendsConfirmation() {
		User actor = User.builder().email("clinic@test.com").firstName("Clinic").build();
		actor.setId(1L);
		Clinic clinic = Clinic.builder().uuid("clinic-1").name("Branch").status(ClinicStatus.VERIFIED).owner(actor)
				.timezone("Asia/Kolkata").latitude(18.52).longitude(73.85).build();
		clinic.setId(100L);
		clinic.setIsActive(true);
		User doctorUser = User.builder().email("doc@test.com").firstName("Ravi").build();
		doctorUser.setId(5L);
		DoctorProfile doctor = DoctorProfile.builder().uuid("doc-1").user(doctorUser).status(DoctorStatus.VERIFIED)
				.build();
		doctor.setId(5L);
		ClinicPetOwner crm = ClinicPetOwner.builder().email("parent@test.com").firstName("Ada").build();
		Pet pet = Pet.builder().uuid("pet-1").name("Miso").clinic(clinic).clinicOwner(crm).build();
		pet.setIsActive(true);
		when(clinicDao.findByUuid("clinic-1")).thenReturn(clinic);
		when(userDao.userByEmail("clinic@test.com")).thenReturn(actor);
		when(clinicDoctorRepository.findByClinic_IdAndDoctor_Uuid(100L, "doc-1"))
				.thenReturn(Optional.of(ClinicDoctor.builder().clinic(clinic).doctor(doctor).isActive(true).build()));
		when(petsRepository.findByUuidIgnoreCase("pet-1")).thenReturn(Optional.of(pet));
		when(doctorAvailabilityRepository.findByDoctor_Id(5L)).thenReturn(Optional.empty());
		when(bookingRepository.findOverlappingForDoctor(any(), any(), any(), any())).thenReturn(List.of());
		when(clinicOwnerUserLinkService.linkOwnerIfUserExists(crm)).thenReturn(crm);
		when(bookingRepository.save(any(Booking.class))).thenAnswer(inv -> {
			Booking saved = inv.getArgument(0);
			saved.setUuid("book-clinic");
			return saved;
		});

		LocalDateTime slot = LocalDateTime.now().plusDays(2).withHour(10).withMinute(0).withSecond(0).withNano(0);
		visitService.createScheduledBooking("clinic-1",
				new ScheduleBookingCreateRequest("pet-1", null, null, "doc-1", slot, null, null, null, null),
				"clinic@test.com");

		verify(zeptoMailService).sendAppointmentConfirmationEmail(eq("parent@test.com"), any(), eq("Branch"),
				eq("Miso"), any(), any(), eq("book-clinic"), any(),
				eq("https://www.google.com/maps/search/?api=1&query=18.52,73.85"), any(), any());
	}

	@Test
	void parentBookingSendsConfirmation() {
		User parent = User.builder().email("parent@test.com").firstName("Ada").build();
		parent.setId(2L);
		Pet pet = Pet.builder().uuid("pet-1").name("Miso").build();
		pet.setIsActive(true);
		parent.setPets(List.of(pet));
		Clinic clinic = Clinic.builder().uuid("clinic-1").name("Branch").status(ClinicStatus.VERIFIED)
				.address("12 Park Road").city("Pune").timezone("Asia/Kolkata").build();
		clinic.setId(100L);
		clinic.setIsActive(true);
		User doctorUser = User.builder().email("doc@test.com").firstName("Ravi").build();
		doctorUser.setId(5L);
		DoctorProfile doctor = DoctorProfile.builder().uuid("doc-1").user(doctorUser).status(DoctorStatus.VERIFIED)
				.build();
		doctor.setId(5L);
		when(userDao.userByEmail("parent@test.com")).thenReturn(parent);
		when(clinicDao.findByUuid("clinic-1")).thenReturn(clinic);
		when(clinicDoctorRepository.findByClinic_IdAndDoctor_Uuid(100L, "doc-1"))
				.thenReturn(Optional.of(ClinicDoctor.builder().clinic(clinic).doctor(doctor).isActive(true).build()));
		when(petsRepository.findOptionalByUuid("pet-1")).thenReturn(Optional.of(pet));
		when(doctorAvailabilityRepository.findByDoctor_Id(5L)).thenReturn(Optional.empty());
		when(bookingRepository.findOverlappingForDoctor(any(), any(), any(), any())).thenReturn(List.of());
		when(bookingRepository.save(any(Booking.class))).thenAnswer(inv -> {
			Booking saved = inv.getArgument(0);
			if (saved.getUuid() == null) {
				saved.setUuid("book-parent");
			}
			return saved;
		});

		LocalDateTime slot = LocalDateTime.now().plusDays(2).withHour(10).withMinute(0).withSecond(0).withNano(0);
		visitService.createParentBooking(
				new ParentBookingCreateRequest("clinic-1", "doc-1", "pet-1", slot, null, null), "parent@test.com");

		verify(zeptoMailService).sendAppointmentConfirmationEmail(eq("parent@test.com"), any(), eq("Branch"),
				eq("Miso"), any(), any(), eq("book-parent"), eq("12 Park Road, Pune"),
				eq("https://www.google.com/maps/search/?api=1&query=12+Park+Road%2C+Pune"), any(), any());
	}
}
