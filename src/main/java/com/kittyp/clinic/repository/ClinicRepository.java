package com.kittyp.clinic.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.kittyp.clinic.entity.Clinic;
import com.kittyp.clinic.enums.ClinicStatus;
import com.kittyp.clinic.enums.PracticeModel;

public interface ClinicRepository extends JpaRepository<Clinic, Long> {

    Clinic findByUuid(String uuid);

    @Query("SELECT c.owner.id FROM Clinic c WHERE c.id = :clinicId")
    Long findOwnerUserIdByClinicId(@Param("clinicId") Long clinicId);

    boolean existsByUuid(String uuid);

    @Query("SELECT c FROM Clinic c LEFT JOIN FETCH c.owner WHERE c.uuid = :uuid")
    Clinic findByUuidFetchOwner(@Param("uuid") String uuid);

    @Query("SELECT c FROM Clinic c LEFT JOIN FETCH c.owner")
    List<Clinic> findAllFetchOwner();

    @Query("""
            SELECT c FROM Clinic c LEFT JOIN FETCH c.owner
            WHERE c.practiceModel = com.kittyp.clinic.enums.PracticeModel.ORGANIZATION
            """)
    List<Clinic> findAllOrganizationFetchOwner();

    @Query("SELECT COUNT(c) FROM Clinic c WHERE c.practiceModel = :practiceModel")
    long countByPracticeModel(@Param("practiceModel") PracticeModel practiceModel);

    @Query("""
            SELECT c FROM Clinic c
            WHERE c.isActive = true
              AND (c.status = com.kittyp.clinic.enums.ClinicStatus.VERIFIED
                   OR c.status = com.kittyp.clinic.enums.ClinicStatus.NOT_REQUIRED)
            """)
    List<Clinic> findDiscoverable();

    Clinic findByOwner_Id(Long ownerUserId);

    List<Clinic> findAllByOwner_Id(Long ownerUserId);

    List<Clinic> findByStatusAndIsActiveTrue(ClinicStatus status);

    List<Clinic> findByWhatsappBusinessAccountId(String whatsappBusinessAccountId);
}
