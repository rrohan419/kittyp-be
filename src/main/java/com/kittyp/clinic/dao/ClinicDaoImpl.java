package com.kittyp.clinic.dao;

import java.util.List;

import org.springframework.core.env.Environment;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Repository;

import com.kittyp.clinic.entity.Clinic;
import com.kittyp.clinic.repository.ClinicRepository;
import com.kittyp.common.constants.ExceptionConstant;
import com.kittyp.common.exception.CustomException;

import lombok.RequiredArgsConstructor;

@Repository
@RequiredArgsConstructor
public class ClinicDaoImpl implements ClinicDao {

    private final ClinicRepository clinicRepository;
    private final Environment env;

    @Override
    public Clinic saveClinic(Clinic clinic) {
        try {
            return clinicRepository.save(clinic);
        } catch (Exception e) {
            throw new CustomException(env.getProperty(ExceptionConstant.ERROR_DATABASE_OPERATION),
                    HttpStatus.INTERNAL_SERVER_ERROR, e);
        }
    }

    @Override
    public Clinic findByUuid(String uuid) {
        return clinicRepository.findByUuid(uuid);
    }

    @Override
    public Clinic findByOwnerUserId(Long ownerUserId) {
        return clinicRepository.findByOwner_Id(ownerUserId);
    }

    @Override
    public List<Clinic> findAllByOwnerUserId(Long ownerUserId) {
        return clinicRepository.findAllByOwner_Id(ownerUserId);
    }
}
