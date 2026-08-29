package com.kittyp.clinic.dao;

import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Repository;

import com.kittyp.clinic.entity.ClinicStaff;
import com.kittyp.clinic.repository.ClinicStaffRepository;

import lombok.RequiredArgsConstructor;

@Repository
@RequiredArgsConstructor
public class ClinicStaffDaoImpl implements ClinicStaffDao {

    private final ClinicStaffRepository clinicStaffRepository;

    @Override
    public ClinicStaff save(ClinicStaff clinicStaff) {
        return clinicStaffRepository.save(clinicStaff);
    }

    @Override
    public boolean isActiveMember(Long clinicId, Long userId) {
        return clinicStaffRepository.existsByClinic_IdAndUser_IdAndIsActiveTrue(clinicId, userId);
    }

    @Override
    public List<ClinicStaff> findActiveByUserId(Long userId) {
        return clinicStaffRepository.findByUser_IdAndIsActiveTrue(userId);
    }

    @Override
    public boolean existsActiveByUserId(Long userId) {
        return clinicStaffRepository.existsByUser_IdAndIsActiveTrue(userId);
    }

    @Override
    public List<ClinicStaff> findActiveByClinicId(Long clinicId) {
        return clinicStaffRepository.findByClinic_IdAndIsActiveTrue(clinicId);
    }

    @Override
    public Optional<ClinicStaff> findLatestByClinicAndUser(Long clinicId, Long userId) {
        return clinicStaffRepository.findFirstByClinic_IdAndUser_IdOrderByIdDesc(clinicId, userId);
    }
}
