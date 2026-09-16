package com.kittyp.booking.service;

import java.time.LocalDateTime;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.kittyp.notification.entity.NotificationLog;
import com.kittyp.notification.enums.NotificationType;
import com.kittyp.notification.repository.NotificationLogRepository;
import com.kittyp.user.entity.Pet;
import com.kittyp.user.entity.User;

import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class VideoCallRingService {

	private static final Logger log = LoggerFactory.getLogger(VideoCallRingService.class);

	private final EntityManager entityManager;
	private final NotificationLogRepository notificationLogRepository;

	@Transactional(propagation = Propagation.REQUIRES_NEW)
	public void persist(Long otherUserId, Long petId, String payload) {
		User other = entityManager.getReference(User.class, otherUserId);
		Pet pet = petId == null ? null : entityManager.getReference(Pet.class, petId);
		notificationLogRepository.saveAndFlush(NotificationLog.builder()
				.user(other)
				.pet(pet)
				.type(NotificationType.VIDEO_CALL_STARTED)
				.payload(payload)
				.sentAt(LocalDateTime.now())
				.build());
		log.info("Persisted video call ring userId={}", otherUserId);
	}
}
