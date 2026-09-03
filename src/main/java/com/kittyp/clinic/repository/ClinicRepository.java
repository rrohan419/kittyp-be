package com.kittyp.clinic.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.kittyp.clinic.entity.Clinic;
import com.kittyp.clinic.enums.ClinicStatus;

public interface ClinicRepository extends JpaRepository<Clinic, Long> {

    Clinic findByUuid(String uuid);

    @Query("SELECT c.owner.id FROM Clinic c WHERE c.id = :clinicId")
    Long findOwnerUserIdByClinicId(@Param("clinicId") Long clinicId);

    boolean existsByUuid(String uuid);

    @Query("SELECT c FROM Clinic c LEFT JOIN FETCH c.owner WHERE c.uuid = :uuid")
    Clinic findByUuidFetchOwner(@Param("uuid") String uuid);

    @Query("SELECT c FROM Clinic c LEFT JOIN FETCH c.owner")
    List<Clinic> findAllFetchOwner();

    /**
     * Organization clinics only: excludes personal practices (owner is an active affiliated doctor).
     */
    @Query("""
            SELECT c FROM Clinic c LEFT JOIN FETCH c.owner
            WHERE c.owner IS NULL
               OR NOT EXISTS (
                 SELECT 1 FROM ClinicDoctor cd
                 WHERE cd.clinic = c
                   AND cd.isActive = true
                   AND cd.doctor.user = c.owner
               )
            """)
    List<Clinic> findAllOrganizationFetchOwner();

    @Query("""
            SELECT COUNT(c) FROM Clinic c
            WHERE c.owner IS NULL
               OR NOT EXISTS (
                 SELECT 1 FROM ClinicDoctor cd
                 WHERE cd.clinic = c
                   AND cd.isActive = true
                   AND cd.doctor.user.id = c.owner.id
               )
            """)
    long countOrganizationClinics();

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
