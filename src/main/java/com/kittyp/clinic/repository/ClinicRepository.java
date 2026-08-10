package com.kittyp.clinic.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import com.kittyp.clinic.entity.Clinic;
import com.kittyp.clinic.enums.ClinicStatus;

public interface ClinicRepository extends JpaRepository<Clinic, Long> {

    Clinic findByUuid(String uuid);

    Clinic findByOwner_Id(Long ownerUserId);

    List<Clinic> findAllByOwner_Id(Long ownerUserId);

    List<Clinic> findByStatusAndIsActiveTrue(ClinicStatus status);

    /** Active, bookable clinics for parent discovery (city/q filtered in service). */
    @Query("""
            SELECT c FROM Clinic c
            WHERE c.isActive = true
              AND c.status <> com.kittyp.clinic.enums.ClinicStatus.SHUTDOWN
              AND c.status <> com.kittyp.clinic.enums.ClinicStatus.REJECTED
            """)
    List<Clinic> findDiscoverable();
}
