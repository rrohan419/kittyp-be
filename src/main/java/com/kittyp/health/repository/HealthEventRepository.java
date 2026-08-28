package com.kittyp.health.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.kittyp.health.entity.HealthEvent;

public interface HealthEventRepository extends JpaRepository<HealthEvent, Long> {

    Optional<HealthEvent> findByUuid(String uuid);

    List<HealthEvent> findByClinic_IdAndPet_UuidOrderByDateDesc(Long clinicId, String petUuid);

    List<HealthEvent> findByClinic_Id(Long clinicId);

    long countDistinctPet_IdByClinic_Id(Long clinicId);
}
