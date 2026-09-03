package com.kittyp.visit.dao;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

import com.kittyp.visit.entity.Visit;
import com.kittyp.visit.enums.VisitStatus;

public interface VisitDao {

    Visit save(Visit visit);

    Optional<Visit> findByUuid(String uuid);

    Optional<Visit> findByUuidAndClinicId(String uuid, Long clinicId);

    List<Visit> findByClinicAndDay(Long clinicId, LocalDateTime from, LocalDateTime to);

    List<Visit> findByClinicStatusAndDay(Long clinicId, VisitStatus status, LocalDateTime from, LocalDateTime to);

    List<Visit> findByDoctorAndDay(Long doctorId, LocalDateTime from, LocalDateTime to);

    List<Visit> findByClinicScheduleBetween(Long clinicId, LocalDateTime from, LocalDateTime to);

    List<Visit> findByDoctorScheduleBetween(Long doctorId, LocalDateTime from, LocalDateTime to);

    List<Visit> findByPetAndClinic(String petUuid, Long clinicId);

    List<Visit> findCompletedByPetUuid(String petUuid);

    List<Visit> findForParentByPetUuids(List<String> petUuids);

    List<Visit> findForParentUser(Long userId, String userUuid, String userEmail, List<String> petUuids);

    List<Visit> findByClinicAndDoctor(Long clinicId, Long doctorId);

    List<Visit> findByDoctor(Long doctorId);

    List<Visit> findByClinicAndStatuses(Long clinicId, Collection<VisitStatus> statuses);
}
