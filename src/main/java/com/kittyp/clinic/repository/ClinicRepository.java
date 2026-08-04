package com.kittyp.clinic.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.kittyp.clinic.entity.Clinic;

public interface ClinicRepository extends JpaRepository<Clinic, Long> {

    Clinic findByUuid(String uuid);

    Clinic findByOwner_Id(Long ownerUserId);

    List<Clinic> findAllByOwner_Id(Long ownerUserId);
}
