package com.kittyp.nutrition.repository;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.kittyp.nutrition.entity.PetFeedingLog;

public interface PetFeedingLogRepository extends JpaRepository<PetFeedingLog, Long>{

    List<PetFeedingLog> findByPetUuidAndLoggedAtBetweenOrderByLoggedAtDesc(
            String petUuid, LocalDateTime from, LocalDateTime to);

    List<PetFeedingLog> findByPetUuidAndLoggedAtGreaterThanEqualAndLoggedAtLessThanOrderByLoggedAtDesc(
            String petUuid, LocalDateTime from, LocalDateTime to);
}
