package com.kittyp.booking.service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.kittyp.booking.dto.IncomingVideoCallModel;
import com.kittyp.booking.dto.VideoCallRingPayload;
import com.kittyp.booking.dto.VideoJoinModel;
import com.kittyp.booking.dto.VideoLiveModel;
import com.kittyp.booking.entity.Booking;
import com.kittyp.booking.enums.BookingMode;
import com.kittyp.booking.enums.BookingStatus;
import com.kittyp.booking.repository.BookingRepository;
import com.kittyp.common.exception.CustomException;
import com.kittyp.common.exception.ResourceNotFoundException;
import com.kittyp.notification.entity.NotificationLog;
import com.kittyp.notification.enums.NotificationType;
import com.kittyp.notification.repository.NotificationLogRepository;
import com.kittyp.user.dao.UserDao;
import com.kittyp.user.entity.Pet;
import com.kittyp.user.entity.User;
import com.kittyp.user.service.UserService;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class BookingVideoService {

	private static final Logger log = LoggerFactory.getLogger(BookingVideoService.class);
	static final String RING_TITLE = "Incoming video consult";
	static final String FCM_TYPE = "VIDEO_CALL";
	static final Duration RING_COOLDOWN = Duration.ofSeconds(90);
	static final Duration INCOMING_WINDOW = Duration.ofMinutes(5);

	private final BookingRepository bookingRepository;
	private final UserDao userDao;
	private final JitsiMeetService jitsiMeetService;
	private final NotificationLogRepository notificationLogRepository;
	private final UserService userService;
	private final VideoCallRingService videoCallRingService;
	private final ObjectMapper objectMapper;

	@Transactional
	public VideoJoinModel join(String email, String bookingUuid) {
		User user = userDao.userByEmail(email);
		Booking booking = bookingRepository.findByUuid(bookingUuid)
				.orElseThrow(() -> new ResourceNotFoundException("booking", "uuid", bookingUuid));
		if (!canJoin(user, booking)) {
			throw new AccessDeniedException("You cannot join this video consult");
		}
		if (booking.getStatus() == BookingStatus.CANCELLED || booking.getStatus() == BookingStatus.NO_SHOW) {
			throw new CustomException("This appointment is no longer active", HttpStatus.BAD_REQUEST);
		}
		if (booking.getMode() != BookingMode.VIDEO) {
			throw new CustomException("This appointment is not a video consult", HttpStatus.BAD_REQUEST);
		}
		if (!booking.isVideoJoinOpen()) {
			throw new CustomException("This video consult window is not open", HttpStatus.CONFLICT);
		}
		boolean doctor = isDoctor(user, booking);
		if (!doctor && !booking.isVideoLive()) {
			throw new CustomException("The doctor is not on the call", HttpStatus.CONFLICT);
		}
		jitsiMeetService.ensureVideoRoom(booking);
		if (doctor) {
			booking.setVideoDoctorHeartbeatAt(LocalDateTime.now());
		}
		bookingRepository.save(booking);
		markOwnRingsRead(user, booking.getUuid());
		if (doctor) {
			ringOtherParties(user, booking);
		}
		return new VideoJoinModel(
				booking.getUuid(),
				booking.getJitsiRoomId(),
				jitsiMeetService.domain(),
				booking.getVideoJoinUrl(),
				displayName(user));
	}

	@Transactional(readOnly = true)
	public IncomingVideoCallModel incoming(String email) {
		User user = userDao.userByEmail(email);
		if (user == null || user.getId() == null) {
			return null;
		}
		return notificationLogRepository
				.findFirstByUser_IdAndTypeAndReadAtIsNullAndSentAtGreaterThanEqualOrderBySentAtDesc(
						user.getId(), NotificationType.VIDEO_CALL_STARTED,
						LocalDateTime.now().minus(INCOMING_WINDOW))
				.map(this::toIncoming)
				.filter(incoming -> incoming != null && isOpenLiveBooking(incoming.bookingUuid()))
				.orElse(null);
	}

	@Transactional(readOnly = true)
	public VideoLiveModel status(String email, String bookingUuid) {
		User user = userDao.userByEmail(email);
		Booking booking = requireBooking(bookingUuid);
		if (!canJoin(user, booking)) {
			throw new AccessDeniedException("You cannot join this video consult");
		}
		return new VideoLiveModel(booking.getUuid(), booking.isVideoLive(), booking.isVideoJoinOpen());
	}

	@Transactional
	public void heartbeat(String email, String bookingUuid) {
		User user = userDao.userByEmail(email);
		Booking booking = requireActiveVideoBooking(bookingUuid);
		if (!isDoctor(user, booking)) {
			throw new AccessDeniedException("Only the doctor can keep this call live");
		}
		booking.setVideoDoctorHeartbeatAt(LocalDateTime.now());
		bookingRepository.save(booking);
	}

	@Transactional
	public void end(String email, String bookingUuid) {
		User user = userDao.userByEmail(email);
		Booking booking = requireBooking(bookingUuid);
		if (!isDoctor(user, booking)) {
			throw new AccessDeniedException("Only the doctor can end this call");
		}
		booking.setVideoDoctorHeartbeatAt(null);
		bookingRepository.save(booking);
		for (User other : otherParties(user, booking)) {
			markRingsRead(other.getId(), booking.getUuid());
		}
	}

	@Transactional
	public void ack(String email, String bookingUuid) {
		User user = userDao.userByEmail(email);
		if (user == null || user.getId() == null || bookingUuid == null || bookingUuid.isBlank()) {
			return;
		}
		markRingsRead(user.getId(), bookingUuid);
	}

	private void ringOtherParties(User joiner, Booking booking) {
		List<User> others = otherParties(joiner, booking);
		if (others.isEmpty()) {
			log.warn("Video join {} has no other party to ring", booking.getUuid());
			return;
		}
		String callerName = displayName(joiner);
		String petName = booking.getPet() == null || booking.getPet().getName() == null
				|| booking.getPet().getName().isBlank()
						? "a pet"
						: booking.getPet().getName();
		Long petId = booking.getPet() == null ? null : booking.getPet().getId();
		for (User other : others) {
			if (onCooldown(other.getId(), booking.getUuid())) {
				continue;
			}
			String joinPath = consultPath(other, booking);
			String payload = writePayload(new VideoCallRingPayload(
					booking.getUuid(), joinPath, callerName, petName, callerPhotoUrl(joiner, booking)));
			if (payload == null) {
				continue;
			}
			try {
				videoCallRingService.persist(other.getId(), petId, payload);
			} catch (Exception e) {
				log.warn("Failed to log video call ring: {}", e.getMessage());
			}
			try {
				if (other.getEmail() != null) {
					userService.sendPushNotification(other.getEmail(), RING_TITLE,
							callerName + " started the video call", joinPath, FCM_TYPE);
				}
			} catch (Exception e) {
				log.warn("Failed to push video call ring: {}", e.getMessage());
			}
		}
	}

	private void markOwnRingsRead(User joiner, String bookingUuid) {
		if (joiner == null || joiner.getId() == null || bookingUuid == null) {
			return;
		}
		markRingsRead(joiner.getId(), bookingUuid);
	}

	private void markRingsRead(Long userId, String bookingUuid) {
		List<NotificationLog> logs = notificationLogRepository
				.findByUser_IdAndTypeAndReadAtIsNullAndPayloadContaining(
						userId, NotificationType.VIDEO_CALL_STARTED, payloadFragment(bookingUuid));
		if (logs == null || logs.isEmpty()) {
			return;
		}
		LocalDateTime now = LocalDateTime.now();
		for (NotificationLog ring : logs) {
			ring.setReadAt(now);
		}
		notificationLogRepository.saveAll(logs);
	}

	private boolean onCooldown(Long userId, String bookingUuid) {
		return notificationLogRepository
				.findFirstByUser_IdAndTypeAndReadAtIsNullAndPayloadContainingAndSentAtGreaterThanEqualOrderBySentAtDesc(
						userId, NotificationType.VIDEO_CALL_STARTED, payloadFragment(bookingUuid),
						LocalDateTime.now().minus(RING_COOLDOWN))
				.isPresent();
	}

	private IncomingVideoCallModel toIncoming(NotificationLog ring) {
		VideoCallRingPayload payload = readPayload(ring.getPayload());
		if (payload == null || payload.bookingUuid() == null || payload.bookingUuid().isBlank()
				|| payload.joinPath() == null || payload.joinPath().isBlank()) {
			return null;
		}
		String caller = payload.callerName() == null || payload.callerName().isBlank()
				? "Someone"
				: payload.callerName();
		return new IncomingVideoCallModel(
				payload.bookingUuid(),
				RING_TITLE,
				caller + " started the video call",
				payload.joinPath(),
				caller,
				payload.callerPhotoUrl());
	}

	private String writePayload(VideoCallRingPayload payload) {
		try {
			return objectMapper.writeValueAsString(payload);
		} catch (JsonProcessingException e) {
			log.warn("Could not serialize video call ring payload: {}", e.getMessage());
			return null;
		}
	}

	private VideoCallRingPayload readPayload(String raw) {
		if (raw == null || raw.isBlank()) {
			return null;
		}
		try {
			return objectMapper.readValue(raw, VideoCallRingPayload.class);
		} catch (Exception e) {
			log.warn("Could not parse video call ring payload: {}", e.getMessage());
			return null;
		}
	}

	private static String payloadFragment(String bookingUuid) {
		return "\"bookingUuid\":\"" + bookingUuid + "\"";
	}

	private static List<User> otherParties(User joiner, Booking booking) {
		Map<Long, User> targets = new LinkedHashMap<>();
		if (joiner == null || joiner.getId() == null || booking == null) {
			return List.of();
		}
		User owner = booking.getOwner();
		User linked = linkedParent(booking);
		User doctorUser = booking.getDoctor() == null ? null : booking.getDoctor().getUser();
		boolean joinerIsDoctor = sameUser(joiner, doctorUser);
		boolean joinerIsParent = sameUser(joiner, owner) || sameUser(joiner, linked)
				|| sameParentUuid(joiner, booking.getPet());
		if (joinerIsDoctor) {
			addTarget(targets, owner);
			addTarget(targets, linked);
		} else if (joinerIsParent) {
			addTarget(targets, doctorUser);
		}
		return new ArrayList<>(targets.values());
	}

	private Booking requireBooking(String bookingUuid) {
		return bookingRepository.findByUuid(bookingUuid)
				.orElseThrow(() -> new ResourceNotFoundException("booking", "uuid", bookingUuid));
	}

	private Booking requireActiveVideoBooking(String bookingUuid) {
		Booking booking = requireBooking(bookingUuid);
		if (booking.getStatus() == BookingStatus.CANCELLED || booking.getStatus() == BookingStatus.NO_SHOW) {
			throw new CustomException("This appointment is no longer active", HttpStatus.BAD_REQUEST);
		}
		if (booking.getMode() != BookingMode.VIDEO) {
			throw new CustomException("This appointment is not a video consult", HttpStatus.BAD_REQUEST);
		}
		return booking;
	}

	private boolean isOpenLiveBooking(String bookingUuid) {
		if (bookingUuid == null || bookingUuid.isBlank()) {
			return false;
		}
		return bookingRepository.findByUuid(bookingUuid)
				.filter(booking -> booking.isVideoLive() && booking.isVideoJoinOpen())
				.isPresent();
	}

	private static String callerPhotoUrl(User joiner, Booking booking) {
		if (!isDoctor(joiner, booking) || booking.getDoctor() == null) {
			return null;
		}
		String fromDoctor = booking.getDoctor().getPhotoUrl();
		if (fromDoctor != null && !fromDoctor.isBlank()) {
			return fromDoctor.trim();
		}
		User doctorUser = booking.getDoctor().getUser();
		if (doctorUser != null && doctorUser.getProfilePictureUrl() != null
				&& !doctorUser.getProfilePictureUrl().isBlank()) {
			return doctorUser.getProfilePictureUrl().trim();
		}
		return null;
	}

	private static boolean isDoctor(User user, Booking booking) {
		return booking != null && booking.getDoctor() != null && sameUser(user, booking.getDoctor().getUser());
	}

	private static User linkedParent(Booking booking) {
		if (booking.getPet() == null || booking.getPet().getClinicOwner() == null) {
			return null;
		}
		return booking.getPet().getClinicOwner().getLinkedUser();
	}

	private static void addTarget(Map<Long, User> targets, User user) {
		if (user != null && user.getId() != null) {
			targets.putIfAbsent(user.getId(), user);
		}
	}

	private static boolean sameUser(User a, User b) {
		return a != null && b != null && a.getId() != null && a.getId().equals(b.getId());
	}

	private static boolean sameParentUuid(User user, Pet pet) {
		return user != null && user.getUuid() != null && pet != null && pet.getParentUserUuid() != null
				&& pet.getParentUserUuid().equalsIgnoreCase(user.getUuid());
	}

	private static String consultPath(User callee, Booking booking) {
		User doctorUser = booking.getDoctor() == null ? null : booking.getDoctor().getUser();
		if (sameUser(callee, doctorUser)) {
			return "/doctor/consult/" + booking.getUuid();
		}
		return "/app/consult/" + booking.getUuid();
	}

	private static boolean canJoin(User user, Booking booking) {
		if (user == null || user.getId() == null) {
			return false;
		}
		if (sameUser(user, booking.getOwner())) {
			return true;
		}
		if (booking.getDoctor() != null && sameUser(user, booking.getDoctor().getUser())) {
			return true;
		}
		if (sameUser(user, linkedParent(booking))) {
			return true;
		}
		return sameParentUuid(user, booking.getPet());
	}

	private static String displayName(User user) {
		if (user == null) {
			return "Guest";
		}
		String name = ((user.getFirstName() == null ? "" : user.getFirstName()) + " "
				+ (user.getLastName() == null ? "" : user.getLastName())).trim();
		return name.isBlank() ? (user.getEmail() == null ? "Guest" : user.getEmail()) : name;
	}
}
