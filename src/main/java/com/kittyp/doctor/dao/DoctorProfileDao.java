package com.kittyp.doctor.dao;

import java.util.List;

import com.kittyp.doctor.entity.DoctorProfile;
import com.kittyp.doctor.enums.DoctorStatus;

public interface DoctorProfileDao {

    DoctorProfile save(DoctorProfile doctorProfile);

    DoctorProfile findByUserId(Long userId);

    DoctorProfile findByUuid(String uuid);

    List<DoctorProfile> findAllOrdered();

    List<DoctorProfile> findByStatus(DoctorStatus status);
}
