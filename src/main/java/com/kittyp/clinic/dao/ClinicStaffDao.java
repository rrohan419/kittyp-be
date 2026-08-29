package com.kittyp.clinic.dao;

import java.util.List;
import java.util.Optional;

import com.kittyp.clinic.entity.ClinicStaff;

public interface ClinicStaffDao {

    ClinicStaff save(ClinicStaff clinicStaff);

    boolean isActiveMember(Long clinicId, Long userId);

    boolean existsActiveByUserId(Long userId);

    List<ClinicStaff> findActiveByUserId(Long userId);

    List<ClinicStaff> findActiveByClinicId(Long clinicId);

    Optional<ClinicStaff> findLatestByClinicAndUser(Long clinicId, Long userId);
}
