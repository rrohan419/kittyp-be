package com.kittyp.user.repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.kittyp.user.entity.PetReminder;

public interface PetReminderRepository extends JpaRepository<PetReminder, Long> {

    List<PetReminder> findByUser_IdAndIsActiveTrueOrderByDueAtAsc(Long userId);

    Optional<PetReminder> findByUuidAndUser_IdAndIsActiveTrue(String uuid, Long userId);

    @Query("""
            SELECT r FROM PetReminder r
            WHERE r.isActive = true
              AND r.sentAt IS NULL
              AND r.dueAt <= :until
            ORDER BY r.dueAt ASC
            """)
    List<PetReminder> findDueUnsent(@Param("until") LocalDateTime until);
}
