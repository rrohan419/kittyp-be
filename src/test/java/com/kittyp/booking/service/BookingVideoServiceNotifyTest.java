package com.kittyp.booking.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.kittyp.booking.dto.VideoJoinModel;
import com.kittyp.booking.dto.VideoLiveModel;
import com.kittyp.common.exception.CustomException;
import com.kittyp.booking.entity.Booking;
import com.kittyp.booking.enums.BookingMode;
import com.kittyp.booking.enums.BookingStatus;
import com.kittyp.booking.repository.BookingRepository;
import com.kittyp.clinic.entity.ClinicPetOwner;
import com.kittyp.doctor.entity.DoctorProfile;
import com.kittyp.notification.entity.NotificationLog;
import com.kittyp.notification.enums.NotificationType;
import com.kittyp.notification.repository.NotificationLogRepository;
import com.kittyp.user.dao.UserDao;
import com.kittyp.user.entity.Pet;
import com.kittyp.user.entity.User;
import com.kittyp.user.service.UserService;

@ExtendWith(MockitoExtension.class)
class BookingVideoServiceNotifyTest {

	@Mock
	private BookingRepository bookingRepository;
	@Mock
	private UserDao userDao;
	@Mock
	private JitsiMeetService jitsiMeetService;
	@Mock
	private NotificationLogRepository notificationLogRepository;
	@Mock
	private UserService userService;
	@Mock
	private VideoCallRingService videoCallRingService;

	private BookingVideoService service;

	@BeforeEach
	void setUp() {
		service = new BookingVideoService(bookingRepository, userDao, jitsiMeetService, notificationLogRepository,
				userService, videoCallRingService, new ObjectMapper());
		org.mockito.Mockito.lenient().when(jitsiMeetService.domain()).thenReturn("meet.jit.si");
		org.mockito.Mockito.lenient().doAnswer(invocation -> {
			Booking booking = invocation.getArgument(0);
			booking.setJitsiRoomId("room1");
			booking.setVideoJoinUrl("https://meet.jit.si/room1");
			return null;
		}).when(jitsiMeetService).ensureVideoRoom(any());
		org.mockito.Mockito.lenient().when(bookingRepository.save(any()))
				.thenAnswer(invocation -> invocation.getArgument(0));
	}

	@Test
	void doctorJoin_ringsOwnerOnce() {
		Fixture fx = fixture();
		when(userDao.userByEmail("asha@kittyp.test")).thenReturn(fx.doctorUser);
		when(bookingRepository.findByUuid("b-1")).thenReturn(Optional.of(fx.booking));

		service.join("asha@kittyp.test", "b-1");

		ArgumentCaptor<String> payload = ArgumentCaptor.forClass(String.class);
		verify(videoCallRingService).persist(eq(10L), isNull(), payload.capture());
		assertTrue(payload.getValue().contains("\"bookingUuid\":\"b-1\""));
		assertTrue(payload.getValue().contains("/app/consult/b-1"));
		verify(userService).sendPushNotification(eq("ravi@kittyp.test"), eq(BookingVideoService.RING_TITLE),
				eq("Asha Doctor started the video call"), eq("/app/consult/b-1"), eq("VIDEO_CALL"));
	}

	@Test
	void doctorJoin_secondJoinInsideCooldown_doesNotRingAgain() {
		Fixture fx = fixture();
		when(userDao.userByEmail("asha@kittyp.test")).thenReturn(fx.doctorUser);
		when(bookingRepository.findByUuid("b-1")).thenReturn(Optional.of(fx.booking));
		when(notificationLogRepository
				.findFirstByUser_IdAndTypeAndReadAtIsNullAndPayloadContainingAndSentAtGreaterThanEqualOrderBySentAtDesc(
						eq(10L), eq(NotificationType.VIDEO_CALL_STARTED), any(), any()))
				.thenReturn(Optional.of(NotificationLog.builder()
						.user(fx.owner)
						.type(NotificationType.VIDEO_CALL_STARTED)
						.payload("{\"bookingUuid\":\"b-1\"}")
						.sentAt(LocalDateTime.now())
						.build()));

		service.join("asha@kittyp.test", "b-1");

		verify(videoCallRingService, never()).persist(any(), any(), any());
		verify(userService, never()).sendPushNotification(any(), any(), any(), any(), any());
	}

	@Test
	void parentJoin_whenDoctorNotLive_conflict() {
		Fixture fx = fixture();
		when(userDao.userByEmail("ravi@kittyp.test")).thenReturn(fx.owner);
		when(bookingRepository.findByUuid("b-1")).thenReturn(Optional.of(fx.booking));

		CustomException ex = assertThrows(CustomException.class, () -> service.join("ravi@kittyp.test", "b-1"));
		assertEquals(HttpStatus.CONFLICT, ex.getHttpStatus());
		verify(videoCallRingService, never()).persist(any(), any(), any());
	}

	@Test
	void parentJoin_whenDoctorLive_returnsRoomWithoutRing() {
		Fixture fx = fixture();
		fx.booking.setVideoDoctorHeartbeatAt(LocalDateTime.now());
		when(userDao.userByEmail("ravi@kittyp.test")).thenReturn(fx.owner);
		when(bookingRepository.findByUuid("b-1")).thenReturn(Optional.of(fx.booking));

		VideoJoinModel model = service.join("ravi@kittyp.test", "b-1");

		assertEquals("room1", model.roomName());
		verify(videoCallRingService, never()).persist(any(), any(), any());
		verify(userService, never()).sendPushNotification(any(), any(), any(), any(), any());
	}

	@Test
	void doctorEnd_clearsLiveAndMarksParentRingsRead() {
		Fixture fx = fixture();
		fx.booking.setVideoDoctorHeartbeatAt(LocalDateTime.now());
		NotificationLog parentRing = NotificationLog.builder()
				.user(fx.owner)
				.type(NotificationType.VIDEO_CALL_STARTED)
				.payload("{\"bookingUuid\":\"b-1\"}")
				.sentAt(LocalDateTime.now())
				.build();
		when(userDao.userByEmail("asha@kittyp.test")).thenReturn(fx.doctorUser);
		when(bookingRepository.findByUuid("b-1")).thenReturn(Optional.of(fx.booking));
		when(notificationLogRepository.findByUser_IdAndTypeAndReadAtIsNullAndPayloadContaining(
				eq(10L), eq(NotificationType.VIDEO_CALL_STARTED), eq("\"bookingUuid\":\"b-1\"")))
				.thenReturn(List.of(parentRing));

		service.end("asha@kittyp.test", "b-1");

		assertNull(fx.booking.getVideoDoctorHeartbeatAt());
		assertNotNull(parentRing.getReadAt());
		assertFalse(fx.booking.isVideoLive());
	}

	@Test
	void incoming_nullWhenBookingNotLive() {
		Fixture fx = fixture();
		when(userDao.userByEmail("ravi@kittyp.test")).thenReturn(fx.owner);
		when(notificationLogRepository
				.findFirstByUser_IdAndTypeAndReadAtIsNullAndSentAtGreaterThanEqualOrderBySentAtDesc(
						eq(10L), eq(NotificationType.VIDEO_CALL_STARTED), any()))
				.thenReturn(Optional.of(NotificationLog.builder()
						.user(fx.owner)
						.type(NotificationType.VIDEO_CALL_STARTED)
						.payload("{\"bookingUuid\":\"b-1\",\"joinPath\":\"/app/consult/b-1\",\"callerName\":\"Asha\",\"petName\":\"Milo\"}")
						.sentAt(LocalDateTime.now())
						.build()));
		when(bookingRepository.findByUuid("b-1")).thenReturn(Optional.of(fx.booking));

		assertNull(service.incoming("ravi@kittyp.test"));
	}

	@Test
	void heartbeat_keepsCallLive() {
		Fixture fx = fixture();
		when(userDao.userByEmail("asha@kittyp.test")).thenReturn(fx.doctorUser);
		when(bookingRepository.findByUuid("b-1")).thenReturn(Optional.of(fx.booking));

		service.heartbeat("asha@kittyp.test", "b-1");

		assertTrue(fx.booking.isVideoLive());
		VideoLiveModel live = service.status("asha@kittyp.test", "b-1");
		assertTrue(live.live());
		assertTrue(live.joinOpen());
	}

	@Test
	void doctorJoin_afterEnd_ringsAgain() {
		Fixture fx = fixture();
		when(userDao.userByEmail("asha@kittyp.test")).thenReturn(fx.doctorUser);
		when(bookingRepository.findByUuid("b-1")).thenReturn(Optional.of(fx.booking));
		when(notificationLogRepository.findByUser_IdAndTypeAndReadAtIsNullAndPayloadContaining(
				eq(10L), eq(NotificationType.VIDEO_CALL_STARTED), eq("\"bookingUuid\":\"b-1\"")))
				.thenReturn(List.of());

		service.end("asha@kittyp.test", "b-1");
		service.join("asha@kittyp.test", "b-1");

		verify(videoCallRingService).persist(eq(10L), isNull(), contains("/app/consult/b-1"));
	}

	@Test
	void doctorJoin_clinicOwnerNull_doesNotNpeOrPush() {
		Fixture fx = fixture();
		fx.booking.setOwner(null);
		when(userDao.userByEmail("asha@kittyp.test")).thenReturn(fx.doctorUser);
		when(bookingRepository.findByUuid("b-1")).thenReturn(Optional.of(fx.booking));

		service.join("asha@kittyp.test", "b-1");

		verify(videoCallRingService, never()).persist(any(), any(), any());
		verify(userService, never()).sendPushNotification(any(), any(), any(), any(), any());
	}

	@Test
	void doctorJoin_ringsLinkedUserWhenOwnerNull() {
		Fixture fx = fixture();
		fx.booking.setOwner(null);
		User linked = user(30L, "linked@kittyp.test", "Lina", "Parent");
		fx.booking.getPet().setClinicOwner(ClinicPetOwner.builder().linkedUser(linked).build());
		when(userDao.userByEmail("asha@kittyp.test")).thenReturn(fx.doctorUser);
		when(bookingRepository.findByUuid("b-1")).thenReturn(Optional.of(fx.booking));

		service.join("asha@kittyp.test", "b-1");

		verify(videoCallRingService).persist(eq(30L), isNull(), contains("/app/consult/b-1"));
		verify(userService).sendPushNotification(eq("linked@kittyp.test"), eq(BookingVideoService.RING_TITLE),
				eq("Asha Doctor started the video call"), eq("/app/consult/b-1"), eq("VIDEO_CALL"));
	}

	@Test
	void join_marksJoinerUnreadRingsRead() {
		Fixture fx = fixture();
		NotificationLog ownRing = NotificationLog.builder()
				.user(fx.doctorUser)
				.type(NotificationType.VIDEO_CALL_STARTED)
				.payload("{\"bookingUuid\":\"b-1\"}")
				.sentAt(LocalDateTime.now().minusMinutes(1))
				.build();
		when(userDao.userByEmail("asha@kittyp.test")).thenReturn(fx.doctorUser);
		when(bookingRepository.findByUuid("b-1")).thenReturn(Optional.of(fx.booking));
		when(notificationLogRepository.findByUser_IdAndTypeAndReadAtIsNullAndPayloadContaining(
				eq(20L), eq(NotificationType.VIDEO_CALL_STARTED), eq("\"bookingUuid\":\"b-1\"")))
				.thenReturn(List.of(ownRing));

		service.join("asha@kittyp.test", "b-1");

		assertNotNull(ownRing.getReadAt());
		verify(notificationLogRepository).saveAll(List.of(ownRing));
	}

	@Test
	void join_stillReturnsRoom_whenRingLogFails() {
		Fixture fx = fixture();
		when(userDao.userByEmail("asha@kittyp.test")).thenReturn(fx.doctorUser);
		when(bookingRepository.findByUuid("b-1")).thenReturn(Optional.of(fx.booking));
		doThrow(new RuntimeException("notification_logs_type_check")).when(videoCallRingService)
				.persist(any(), any(), any());

		VideoJoinModel model = service.join("asha@kittyp.test", "b-1");

		assertEquals("b-1", model.bookingUuid());
		assertEquals("room1", model.roomName());
	}

	@Test
	void join_beforeSlotStart_ok() {
		Fixture fx = fixture(LocalDateTime.now().plusHours(1));
		when(userDao.userByEmail("asha@kittyp.test")).thenReturn(fx.doctorUser);
		when(bookingRepository.findByUuid("b-1")).thenReturn(Optional.of(fx.booking));

		VideoJoinModel model = service.join("asha@kittyp.test", "b-1");

		assertEquals("room1", model.roomName());
		assertTrue(fx.booking.isVideoJoinOpen());
	}

	@Test
	void parentJoin_beforeSlotStart_whenDoctorLive_ok() {
		Fixture fx = fixture(LocalDateTime.now().plusHours(1));
		fx.booking.setVideoDoctorHeartbeatAt(LocalDateTime.now());
		when(userDao.userByEmail("ravi@kittyp.test")).thenReturn(fx.owner);
		when(bookingRepository.findByUuid("b-1")).thenReturn(Optional.of(fx.booking));

		VideoJoinModel model = service.join("ravi@kittyp.test", "b-1");

		assertEquals("room1", model.roomName());
	}

	@Test
	void join_afterThirtyMinutes_conflict() {
		Fixture fx = fixture(LocalDateTime.now().minusMinutes(31));
		when(userDao.userByEmail("asha@kittyp.test")).thenReturn(fx.doctorUser);
		when(bookingRepository.findByUuid("b-1")).thenReturn(Optional.of(fx.booking));

		CustomException ex = assertThrows(CustomException.class, () -> service.join("asha@kittyp.test", "b-1"));
		assertEquals(HttpStatus.CONFLICT, ex.getHttpStatus());
		assertEquals("This video consult window is not open", ex.getMessage());
	}

	@Test
	void parentJoin_whenDoctorLiveButWindowClosed_conflict() {
		Fixture fx = fixture(LocalDateTime.now().minusMinutes(31));
		fx.booking.setVideoDoctorHeartbeatAt(LocalDateTime.now());
		when(userDao.userByEmail("ravi@kittyp.test")).thenReturn(fx.owner);
		when(bookingRepository.findByUuid("b-1")).thenReturn(Optional.of(fx.booking));

		CustomException ex = assertThrows(CustomException.class, () -> service.join("ravi@kittyp.test", "b-1"));
		assertEquals(HttpStatus.CONFLICT, ex.getHttpStatus());
		assertEquals("This video consult window is not open", ex.getMessage());
	}

	@Test
	void doctorJoin_includesCallerPhotoUrl() {
		Fixture fx = fixture();
		fx.booking.getDoctor().setPhotoUrl("https://cdn.kittyp.test/asha.jpg");
		when(userDao.userByEmail("asha@kittyp.test")).thenReturn(fx.doctorUser);
		when(bookingRepository.findByUuid("b-1")).thenReturn(Optional.of(fx.booking));

		service.join("asha@kittyp.test", "b-1");

		ArgumentCaptor<String> payload = ArgumentCaptor.forClass(String.class);
		verify(videoCallRingService).persist(eq(10L), isNull(), payload.capture());
		assertTrue(payload.getValue().contains("\"callerPhotoUrl\":\"https://cdn.kittyp.test/asha.jpg\""));
	}

	@Test
	void incoming_nullWhenWindowClosed() {
		Fixture fx = fixture(LocalDateTime.now().minusMinutes(31));
		fx.booking.setVideoDoctorHeartbeatAt(LocalDateTime.now());
		when(userDao.userByEmail("ravi@kittyp.test")).thenReturn(fx.owner);
		when(notificationLogRepository
				.findFirstByUser_IdAndTypeAndReadAtIsNullAndSentAtGreaterThanEqualOrderBySentAtDesc(
						eq(10L), eq(NotificationType.VIDEO_CALL_STARTED), any()))
				.thenReturn(Optional.of(NotificationLog.builder()
						.user(fx.owner)
						.type(NotificationType.VIDEO_CALL_STARTED)
						.payload("{\"bookingUuid\":\"b-1\",\"joinPath\":\"/app/consult/b-1\",\"callerName\":\"Asha\",\"petName\":\"Milo\"}")
						.sentAt(LocalDateTime.now())
						.build()));
		when(bookingRepository.findByUuid("b-1")).thenReturn(Optional.of(fx.booking));

		assertNull(service.incoming("ravi@kittyp.test"));
	}

	private static Fixture fixture() {
		return fixture(LocalDateTime.now().minusMinutes(1));
	}

	private static Fixture fixture(LocalDateTime start) {
		User owner = user(10L, "ravi@kittyp.test", "Ravi", "Parent");
		User doctorUser = user(20L, "asha@kittyp.test", "Asha", "Doctor");
		DoctorProfile doctor = DoctorProfile.builder().uuid("doc-1").user(doctorUser).build();
		Pet pet = Pet.builder().uuid("pet-1").name("Milo").build();
		Booking booking = Booking.builder()
				.uuid("b-1")
				.owner(owner)
				.doctor(doctor)
				.pet(pet)
				.mode(BookingMode.VIDEO)
				.status(BookingStatus.CONFIRMED)
				.slotStart(start)
				.slotEnd(start.plusMinutes(30))
				.build();
		return new Fixture(owner, doctorUser, booking);
	}

	private static User user(Long id, String email, String first, String last) {
		User user = User.builder().uuid("u-" + id).email(email).password("x").firstName(first).lastName(last).build();
		user.setId(id);
		return user;
	}

	private record Fixture(User owner, User doctorUser, Booking booking) {
	}
}
