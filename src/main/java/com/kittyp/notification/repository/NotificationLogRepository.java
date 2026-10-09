package com.kittyp.notification.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.kittyp.notification.entity.NotificationLog;
import com.kittyp.notification.enums.NotificationType;

public interface NotificationLogRepository extends JpaRepository<NotificationLog, Long> {

	boolean existsByUser_IdAndTypeAndPayloadContaining(Long userId, NotificationType type, String payloadPart);
}
