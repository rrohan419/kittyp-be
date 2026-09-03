package com.kittyp.clinic.dao;

import java.util.List;

import com.kittyp.clinic.entity.Clinic;

public interface ClinicDao {

    Clinic saveClinic(Clinic clinic);

    Clinic findByUuid(String uuid);

    /** Owner user id only — no User entity load (avoids lazy / decrypt on skip checks). */
    Long findOwnerUserId(Long clinicId);

    Clinic findByOwnerUserId(Long ownerUserId);

    List<Clinic> findAllByOwnerUserId(Long ownerUserId);

    List<Clinic> findAll();

    List<Clinic> findAllFetchOwner();
}
