package com.kittyp.doctor.dao;

import java.util.List;

import org.springframework.core.env.Environment;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Repository;

import com.kittyp.common.constants.ExceptionConstant;
import com.kittyp.common.exception.CustomException;
import com.kittyp.doctor.entity.DoctorProfile;
import com.kittyp.doctor.enums.DoctorStatus;
import com.kittyp.doctor.repository.DoctorProfileRepository;

import lombok.RequiredArgsConstructor;

@Repository
@RequiredArgsConstructor
public class DoctorProfileDaoImpl implements DoctorProfileDao {

    private final DoctorProfileRepository doctorProfileRepository;
    private final Environment env;

    @Override
    public DoctorProfile save(DoctorProfile doctorProfile) {
        try {
            return doctorProfileRepository.save(doctorProfile);
        } catch (Exception e) {
            throw new CustomException(env.getProperty(ExceptionConstant.ERROR_DATABASE_OPERATION),
                    HttpStatus.INTERNAL_SERVER_ERROR, e);
        }
    }

    @Override
    public DoctorProfile findByUserId(Long userId) {
        return doctorProfileRepository.findByUser_Id(userId);
    }

    @Override
    public DoctorProfile findByUuid(String uuid) {
        return doctorProfileRepository.findByUuid(uuid)
                .orElseThrow(() -> new CustomException("Doctor profile not found", HttpStatus.NOT_FOUND));
    }

    @Override
    public List<DoctorProfile> findAllOrdered() {
        return doctorProfileRepository.findAllByOrderBySubmittedAtDesc();
    }

    @Override
    public List<DoctorProfile> findByStatus(DoctorStatus status) {
        return doctorProfileRepository.findByStatusOrderBySubmittedAtDesc(status);
    }
}
