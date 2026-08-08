package com.kittyp.health.dao;

import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Repository;

import com.kittyp.health.entity.HealthEvent;
import com.kittyp.health.repository.HealthEventRepository;

import lombok.RequiredArgsConstructor;

@Repository
@RequiredArgsConstructor
public class HealthEventDaoImpl implements HealthEventDao {

    private final HealthEventRepository healthEventRepository;

    @Override
    public HealthEvent save(HealthEvent healthEvent) {
        return healthEventRepository.save(healthEvent);
    }

    @Override
    public Optional<HealthEvent> findByUuid(String uuid) {
        return healthEventRepository.findByUuid(uuid);
    }

    @Override
    public List<HealthEvent> findByClinicAndPet(Long clinicId, String petUuid) {
        return healthEventRepository.findByClinic_IdAndPet_UuidOrderByDateDesc(clinicId, petUuid);
    }

    @Override
    public List<HealthEvent> findByClinic(Long clinicId) {
        return healthEventRepository.findByClinic_Id(clinicId);
    }

    @Override
    public long countDistinctPetsByClinic(Long clinicId) {
        return healthEventRepository.countDistinctPet_IdByClinic_Id(clinicId);
    }
}
