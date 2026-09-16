package com.kittyp.notification.repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.kittyp.notification.entity.NotificationLog;
import com.kittyp.notification.enums.NotificationType;

public interface NotificationLogRepository extends JpaRepository<NotificationLog, Long> {

	Optional<NotificationLog> findFirstByUser_IdAndTypeAndReadAtIsNullAndPayloadContainingAndSentAtGreaterThanEqualOrderBySentAtDesc(
			Long userId, NotificationType type, String payloadFragment, LocalDateTime sentAt);

	Optional<NotificationLog> findFirstByUser_IdAndTypeAndReadAtIsNullAndSentAtGreaterThanEqualOrderBySentAtDesc(
			Long userId, NotificationType type, LocalDateTime sentAt);

	List<NotificationLog> findByUser_IdAndTypeAndReadAtIsNullAndPayloadContaining(
			Long userId, NotificationType type, String payloadFragment);
}
