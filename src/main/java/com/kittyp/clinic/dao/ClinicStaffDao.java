package com.kittyp.clinic.dao;

import java.util.List;

import com.kittyp.clinic.entity.ClinicStaff;

public interface ClinicStaffDao {

    ClinicStaff save(ClinicStaff clinicStaff);

    boolean isActiveMember(Long clinicId, Long userId);

    List<ClinicStaff> findActiveByUserId(Long userId);
}
