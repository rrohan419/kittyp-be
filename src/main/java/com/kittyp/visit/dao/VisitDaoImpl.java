package com.kittyp.visit.dao;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Repository;

import com.kittyp.visit.entity.Visit;
import com.kittyp.visit.enums.VisitStatus;
import com.kittyp.visit.repository.VisitRepository;

import lombok.RequiredArgsConstructor;

@Repository
@RequiredArgsConstructor
public class VisitDaoImpl implements VisitDao {

    private final VisitRepository visitRepository;

    @Override
    public Visit save(Visit visit) {
        return visitRepository.save(visit);
    }

    @Override
    public Optional<Visit> findByUuid(String uuid) {
        return visitRepository.findByUuid(uuid);
    }

    @Override
    public Optional<Visit> findByUuidAndClinicId(String uuid, Long clinicId) {
        return visitRepository.findByUuidAndClinic_Id(uuid, clinicId);
    }

    @Override
    public List<Visit> findByClinicAndDay(Long clinicId, LocalDateTime from, LocalDateTime to) {
        return visitRepository.findByClinic_IdAndCreatedAtBetweenOrderByCreatedAtDesc(clinicId, from, to);
    }

    @Override
    public List<Visit> findByClinicStatusAndDay(Long clinicId, VisitStatus status, LocalDateTime from,
            LocalDateTime to) {
        return visitRepository.findByClinic_IdAndStatusAndCreatedAtBetweenOrderByUrgencyDescCreatedAtAsc(
                clinicId, status, from, to);
    }

    @Override
    public List<Visit> findByDoctorAndDay(Long doctorId, LocalDateTime from, LocalDateTime to) {
        return visitRepository.findByDoctor_IdAndCreatedAtBetweenOrderByUrgencyDescCreatedAtAsc(doctorId, from, to);
    }

    @Override
    public List<Visit> findByPetAndClinic(String petUuid, Long clinicId) {
        return visitRepository.findByPet_UuidAndClinic_IdOrderByCreatedAtDesc(petUuid, clinicId);
    }

    @Override
    public List<Visit> findCompletedByPetUuid(String petUuid) {
        return visitRepository.findCompletedByPetUuid(petUuid);
    }

    @Override
    public List<Visit> findForParentByPetUuids(List<String> petUuids) {
        if (petUuids == null || petUuids.isEmpty()) {
            return List.of();
        }
        return visitRepository.findForParentByPetUuids(petUuids);
    }

    @Override
    public List<Visit> findForParentUser(Long userId, String userEmail, List<String> petUuids) {
        if (userId == null) {
            return List.of();
        }
        String email = userEmail == null || userEmail.isBlank() ? "__none__" : userEmail.trim();
        if (petUuids == null || petUuids.isEmpty()) {
            return visitRepository.findForParentByLinkedUserOrEmail(userId, email);
        }
        return visitRepository.findForParentUser(userId, email, petUuids);
    }

    @Override
    public List<Visit> findByClinicAndDoctor(Long clinicId, Long doctorId) {
        return visitRepository.findByClinic_IdAndDoctor_IdOrderByCreatedAtDesc(clinicId, doctorId);
    }

    @Override
    public List<Visit> findByDoctor(Long doctorId) {
        return visitRepository.findByDoctor_IdOrderByCreatedAtDesc(doctorId);
    }
}
