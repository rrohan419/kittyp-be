package com.kittyp.visit.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.kittyp.booking.entity.Booking;
import com.kittyp.booking.repository.BookingRepository;
import com.kittyp.clinic.dao.ClinicDao;
import com.kittyp.clinic.dao.ClinicStaffDao;
import com.kittyp.clinic.entity.Clinic;
import com.kittyp.clinic.entity.ClinicPetOwner;
import com.kittyp.clinic.enums.ClinicStatus;
import com.kittyp.clinic.repository.ClinicDoctorRepository;
import com.kittyp.clinic.service.ClinicOwnerUserLinkService;
import com.kittyp.doctor.dao.DoctorProfileDao;
import com.kittyp.doctor.repository.DoctorReviewRepository;
import com.kittyp.email.service.ZeptoMailService;
import com.kittyp.notification.entity.NotificationLog;
import com.kittyp.notification.enums.NotificationType;
import com.kittyp.notification.repository.NotificationLogRepository;
import com.kittyp.notification.service.OutboundMessageService;
import com.kittyp.user.dao.UserDao;
import com.kittyp.user.entity.Pet;
import com.kittyp.user.entity.User;
import com.kittyp.user.repository.PetsRepository;
import com.kittyp.user.service.UserService;
import com.kittyp.visit.dao.VisitDao;
import com.kittyp.visit.dto.VisitDtos.ScheduleBookingCreateRequest;
import com.kittyp.visit.dto.VisitDtos.WalkInCreateRequest;
import com.kittyp.visit.entity.Visit;

@ExtendWith(MockitoExtension.class)
class VisitServiceImplDoctorBookedParentTest {

	@Mock
	private VisitDao visitDao;
	@Mock
	private ClinicDao clinicDao;
	@Mock
	private ClinicStaffDao clinicStaffDao;
	@Mock
	private ClinicDoctorRepository clinicDoctorRepository;
	@Mock
	private DoctorProfileDao doctorProfileDao;
	@Mock
	private DoctorReviewRepository doctorReviewRepository;
	@Mock
	private PetsRepository petsRepository;
	@Mock
	private UserDao userDao;
	@Mock
	private ClinicOwnerUserLinkService clinicOwnerUserLinkService;
	@Mock
	private NotificationLogRepository notificationLogRepository;
	@Mock
	private BookingRepository bookingRepository;
	@Mock
	private UserService userService;
	@Mock
	private OutboundMessageService outboundMessageService;
	@Mock
	private ParentBookingEnrollmentService parentBookingEnrollmentService;
	@Mock
	private ZeptoMailService zeptoMailService;

	@InjectMocks
	private VisitServiceImpl visitService;

	private Clinic clinic;
	private User parent;
	private ClinicPetOwner crmOwner;
	private Pet pet;

	@BeforeEach
	void setUp() {
		User staff = User.builder().email("staff@example.com").password("x").uuid("staff-1").firstName("Sam").build();
		staff.setId(1L);
		clinic = Clinic.builder().uuid("clinic-v").name("Branch").status(ClinicStatus.VERIFIED).owner(staff).build();
		clinic.setId(30L);
		parent = User.builder().email("pat@example.com").password("x").uuid("parent-1").firstName("Pat").build();
		parent.setId(8L);
		crmOwner = ClinicPetOwner.builder()
				.uuid("owner-1")
				.clinic(clinic)
				.firstName("Pat")
				.email("pat@example.com")
				.phone("9999999999")
				.linkedUser(parent)
				.build();
		crmOwner.setIsActive(true);
		pet = Pet.builder().uuid("pet-1").name("Milo").clinic(clinic).clinicOwner(crmOwner).build();
		pet.setIsActive(true);

		when(clinicDao.findByUuid("clinic-v")).thenReturn(clinic);
		when(userDao.userByEmail("staff@example.com")).thenReturn(staff);
		when(petsRepository.findByUuidIgnoreCase("pet-1")).thenReturn(Optional.of(pet));
	}

	@Test
	void walkIn_linksParent_sendsAppointmentEmail_notReminder() {
		when(clinicOwnerUserLinkService.linkOwnerIfUserExists(crmOwner)).thenReturn(crmOwner);
		when(visitDao.save(any(Visit.class))).thenAnswer(inv -> inv.getArgument(0));
		when(doctorReviewRepository.findByVisit_Uuid(anyString())).thenReturn(Optional.empty());

		visitService.createWalkIn("clinic-v", new WalkInCreateRequest("pet-1", null, null, null, null, null),
				"staff@example.com");

		ArgumentCaptor<Visit> saved = ArgumentCaptor.forClass(Visit.class);
		verify(visitDao).save(saved.capture());
		assertSame(crmOwner, saved.getValue().getClinicOwner());
		verify(userService).sendPushNotification(eq("pat@example.com"), anyString(), anyString());
		verify(zeptoMailService).sendAppointmentBookedEmail(eq("pat@example.com"), eq("Pat"), eq("Branch"), eq("Milo"),
				eq("Now"), anyString());
		verify(notificationLogRepository, never()).save(any());
		verify(outboundMessageService, never()).trySendAppointmentNotice(any(), any(), any(), any(), any());
	}

	@Test
	void schedule_setsBookingOwner_andNotifiesOnce() {
		when(clinicOwnerUserLinkService.linkOwnerIfUserExists(crmOwner)).thenReturn(crmOwner);
		when(bookingRepository.save(any(Booking.class))).thenAnswer(inv -> {
			Booking saved = inv.getArgument(0);
			saved.setUuid("book-1");
			return saved;
		});

		LocalDateTime slot = LocalDateTime.now().plusDays(1).withHour(10).withMinute(0).withSecond(0).withNano(0);
		visitService.createScheduledBooking("clinic-v",
				new ScheduleBookingCreateRequest("pet-1", null, null, null, slot, null, 30, null, null),
				"staff@example.com");

		ArgumentCaptor<Booking> saved = ArgumentCaptor.forClass(Booking.class);
		verify(bookingRepository).save(saved.capture());
		assertEquals(parent.getId(), saved.getValue().getOwner().getId());

		ArgumentCaptor<NotificationLog> logged = ArgumentCaptor.forClass(NotificationLog.class);
		verify(notificationLogRepository).save(logged.capture());
		assertEquals(NotificationType.BOOKING_CREATED, logged.getValue().getType());
		assertEquals(parent.getId(), logged.getValue().getUser().getId());
		assertTrue(logged.getValue().getPayload().contains("booking:book-1"));
		verify(zeptoMailService).sendAppointmentBookedEmail(eq("pat@example.com"), eq("Pat"), eq("Branch"), eq("Milo"),
				eq(slot.toString()), anyString());
		verify(outboundMessageService, never()).trySendAppointmentNotice(any(), any(), any(), any(), any());
	}

	@Test
	void guest_savesWithoutParentNotice() {
		crmOwner.setLinkedUser(null);
		when(clinicOwnerUserLinkService.linkOwnerIfUserExists(crmOwner)).thenReturn(crmOwner);
		when(userDao.findOptionalByPetUuid("pet-1")).thenReturn(Optional.empty());
		when(visitDao.save(any(Visit.class))).thenAnswer(inv -> inv.getArgument(0));
		when(doctorReviewRepository.findByVisit_Uuid(anyString())).thenReturn(Optional.empty());

		visitService.createWalkIn("clinic-v", new WalkInCreateRequest("pet-1", null, null, null, null, null),
				"staff@example.com");

		verify(visitDao).save(any(Visit.class));
		verify(userService, never()).sendPushNotification(anyString(), anyString(), anyString());
		verify(zeptoMailService, never()).sendAppointmentBookedEmail(any(), any(), any(), any(), any(), any());
		verify(notificationLogRepository, never()).save(any());
	}

	@Test
	void mailOrPushFailure_doesNotFailTheBooking() {
		when(clinicOwnerUserLinkService.linkOwnerIfUserExists(crmOwner)).thenReturn(crmOwner);
		when(bookingRepository.save(any(Booking.class))).thenAnswer(inv -> {
			Booking saved = inv.getArgument(0);
			saved.setUuid("book-2");
			return saved;
		});
		doThrow(new RuntimeException("push down")).when(userService).sendPushNotification(anyString(), anyString(),
				anyString());
		doThrow(new RuntimeException("mail down")).when(zeptoMailService).sendAppointmentBookedEmail(any(), any(),
				any(), any(), any(), any());

		LocalDateTime slot = LocalDateTime.now().plusDays(2).withHour(11).withMinute(0).withSecond(0).withNano(0);
		assertNotNull(visitService.createScheduledBooking("clinic-v",
				new ScheduleBookingCreateRequest("pet-1", null, null, null, slot, null, 30, null, null),
				"staff@example.com"));
	}

	@Test
	void repeatedBookingNotice_isNotSentTwice() {
		when(clinicOwnerUserLinkService.linkOwnerIfUserExists(crmOwner)).thenReturn(crmOwner);
		when(bookingRepository.save(any(Booking.class))).thenAnswer(inv -> {
			Booking saved = inv.getArgument(0);
			saved.setUuid("book-3");
			return saved;
		});
		when(notificationLogRepository.existsByUser_IdAndTypeAndPayloadContaining(eq(8L),
				eq(NotificationType.BOOKING_CREATED), eq("booking:book-3"))).thenReturn(false, true);

		LocalDateTime slot = LocalDateTime.now().plusDays(3).withHour(12).withMinute(0).withSecond(0).withNano(0);
		ScheduleBookingCreateRequest request = new ScheduleBookingCreateRequest("pet-1", null, null, null, slot, null,
				30, null, null);
		visitService.createScheduledBooking("clinic-v", request, "staff@example.com");
		visitService.createScheduledBooking("clinic-v", request, "staff@example.com");

		verify(notificationLogRepository, times(1)).save(any());
		verify(zeptoMailService, times(1)).sendAppointmentBookedEmail(any(), any(), any(), any(), any(), any());
		verify(userService, times(1)).sendPushNotification(anyString(), anyString(), anyString());
	}
}
