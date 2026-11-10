package com.kittyp.vaccine.repository;

import java.time.LocalDate;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.kittyp.vaccine.entity.PetVaccineSchedule;

import jakarta.persistence.LockModeType;

public interface PetVaccineScheduleRepository extends JpaRepository<PetVaccineSchedule, Long> {

    List<PetVaccineSchedule> findByPet_Uuid(String petUuid);

    List<PetVaccineSchedule> findByCompletedFalseAndDueDateLessThanEqual(LocalDate dueDate);

    Page<PetVaccineSchedule> findPageByCompletedFalseAndDueDateLessThanEqual(LocalDate dueDate, Pageable pageable);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            SELECT s FROM PetVaccineSchedule s
            WHERE s.completed = false
              AND s.isActive = true
              AND (s.reminderSent = false OR s.reminderSent IS NULL)
              AND s.dueDate BETWEEN :today AND :until
            ORDER BY s.dueDate, s.id
            """)
    List<PetVaccineSchedule> findPendingReminders(@Param("today") LocalDate today, @Param("until") LocalDate until);
}
