package com.kittyp.clinic.dao;

import java.util.List;

import com.kittyp.clinic.entity.Clinic;

public interface ClinicDao {

    Clinic saveClinic(Clinic clinic);

    Clinic findByUuid(String uuid);

    Clinic findByOwnerUserId(Long ownerUserId);

    List<Clinic> findAllByOwnerUserId(Long ownerUserId);

    List<Clinic> findAll();

    List<Clinic> findAllFetchOwner();

    /** Org clinics for admin list (excludes personal practices). */
    List<Clinic> findAllOrganizationFetchOwner();
}
