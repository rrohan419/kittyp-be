package com.kittyp.health.dao;

import java.util.List;
import java.util.Optional;

import com.kittyp.health.entity.HealthEvent;

public interface HealthEventDao {

    HealthEvent save(HealthEvent healthEvent);

    Optional<HealthEvent> findByUuid(String uuid);

    List<HealthEvent> findByClinicAndPet(Long clinicId, String petUuid);

    List<HealthEvent> findByClinic(Long clinicId);

    long countDistinctPetsByClinic(Long clinicId);
}
