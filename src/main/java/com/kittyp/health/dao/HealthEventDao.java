package com.kittyp.health.dao;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import com.kittyp.health.entity.HealthEvent;

public interface HealthEventDao {

    HealthEvent save(HealthEvent healthEvent);

    Optional<HealthEvent> findByUuid(String uuid);

    List<HealthEvent> findByClinicAndPet(Long clinicId, String petUuid);

    List<HealthEvent> findByClinic(Long clinicId);

    Page<HealthEvent> findByClinic(Long clinicId, Pageable pageable);

    long countDistinctPetsByClinic(Long clinicId);
}
