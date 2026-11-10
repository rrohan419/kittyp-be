package com.kittyp.user.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.kittyp.user.entity.PetWeightLog;

public interface PetWeightLogRepository extends JpaRepository<PetWeightLog, Long> {

    List<PetWeightLog> findByPet_UuidOrderByRecordedAtDescIdDesc(String petUuid);

    Optional<PetWeightLog> findFirstByPet_UuidOrderByRecordedAtDescIdDesc(String petUuid);

    Optional<PetWeightLog> findByVisit_Id(Long visitId);
}
