package com.kittyp.notification.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.kittyp.notification.entity.NotificationLog;

public interface NotificationLogRepository extends JpaRepository<NotificationLog, Long> {
}
