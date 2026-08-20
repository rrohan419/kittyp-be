package com.kittyp.clinic.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.kittyp.clinic.entity.Clinic;
import com.kittyp.clinic.enums.ClinicStatus;

public interface ClinicRepository extends JpaRepository<Clinic, Long> {

    Clinic findByUuid(String uuid);

    boolean existsByUuid(String uuid);

    @Query("SELECT c FROM Clinic c LEFT JOIN FETCH c.owner WHERE c.uuid = :uuid")
    Clinic findByUuidFetchOwner(@Param("uuid") String uuid);

    Clinic findByOwner_Id(Long ownerUserId);

    List<Clinic> findAllByOwner_Id(Long ownerUserId);

    List<Clinic> findByStatusAndIsActiveTrue(ClinicStatus status);

    /** Active, admin-verified clinics for parent discovery (city/q filtered in service). */
    @Query("""
            SELECT c FROM Clinic c
            WHERE c.isActive = true
              AND c.status = com.kittyp.clinic.enums.ClinicStatus.VERIFIED
            """)
    List<Clinic> findDiscoverable();
}
