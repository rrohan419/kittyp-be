package com.kittyp.visit.repository;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.kittyp.visit.entity.Visit;
import com.kittyp.visit.enums.VisitStatus;

public interface VisitRepository extends JpaRepository<Visit, Long> {

    @EntityGraph(attributePaths = { "pet", "clinicOwner", "doctor", "doctor.user", "clinic" })
    Optional<Visit> findByUuid(String uuid);

    @EntityGraph(attributePaths = { "pet", "clinicOwner", "doctor", "doctor.user", "clinic" })
    Optional<Visit> findByUuidAndClinic_Id(String uuid, Long clinicId);

    @EntityGraph(attributePaths = { "pet", "clinicOwner", "doctor", "doctor.user", "clinic" })
    List<Visit> findByClinic_IdAndCreatedAtBetweenOrderByCreatedAtDesc(
            Long clinicId, LocalDateTime from, LocalDateTime to);

    @EntityGraph(attributePaths = { "pet", "clinicOwner", "doctor", "doctor.user", "clinic" })
    List<Visit> findByClinic_IdAndStatusAndCreatedAtBetweenOrderByUrgencyDescCreatedAtAsc(
            Long clinicId, VisitStatus status, LocalDateTime from, LocalDateTime to);

    @EntityGraph(attributePaths = { "pet", "clinicOwner", "doctor", "doctor.user", "clinic" })
    List<Visit> findByDoctor_IdAndCreatedAtBetweenOrderByUrgencyDescCreatedAtAsc(
            Long doctorId, LocalDateTime from, LocalDateTime to);

    @EntityGraph(attributePaths = { "pet", "clinicOwner", "doctor", "doctor.user", "clinic" })
    List<Visit> findByPet_UuidAndClinic_IdOrderByCreatedAtDesc(String petUuid, Long clinicId);

    @EntityGraph(attributePaths = { "pet", "clinicOwner", "doctor", "doctor.user", "clinic" })
    @Query("""
            SELECT v FROM Visit v
            WHERE v.pet.uuid = :petUuid
              AND v.isActive = true
              AND v.status IN (
                  com.kittyp.visit.enums.VisitStatus.WAITLIST,
                  com.kittyp.visit.enums.VisitStatus.CHECKED_IN,
                  com.kittyp.visit.enums.VisitStatus.IN_PROGRESS,
                  com.kittyp.visit.enums.VisitStatus.CHECKING_OUT,
                  com.kittyp.visit.enums.VisitStatus.COMPLETED,
                  com.kittyp.visit.enums.VisitStatus.CANCELLED,
                  com.kittyp.visit.enums.VisitStatus.NO_SHOW
              )
            ORDER BY v.createdAt DESC
            """)
    List<Visit> findCompletedByPetUuid(@Param("petUuid") String petUuid);

    @EntityGraph(attributePaths = { "pet", "clinicOwner", "doctor", "doctor.user", "clinic" })
    @Query("""
            SELECT v FROM Visit v
            WHERE v.pet.uuid IN :petUuids
              AND v.isActive = true
              AND v.status IN (
                  com.kittyp.visit.enums.VisitStatus.WAITLIST,
                  com.kittyp.visit.enums.VisitStatus.CHECKED_IN,
                  com.kittyp.visit.enums.VisitStatus.IN_PROGRESS,
                  com.kittyp.visit.enums.VisitStatus.CHECKING_OUT,
                  com.kittyp.visit.enums.VisitStatus.COMPLETED,
                  com.kittyp.visit.enums.VisitStatus.CANCELLED,
                  com.kittyp.visit.enums.VisitStatus.NO_SHOW
              )
            ORDER BY v.createdAt DESC
            """)
    List<Visit> findForParentByPetUuids(@Param("petUuids") List<String> petUuids);

    @EntityGraph(attributePaths = { "pet", "clinicOwner", "doctor", "doctor.user", "clinic" })
    @Query("""
            SELECT DISTINCT v FROM Visit v
            LEFT JOIN v.pet p
            LEFT JOIN v.clinicOwner co
            LEFT JOIN co.linkedUser lu
            WHERE v.isActive = true
              AND (
                  p.uuid IN :petUuids
                  OR (p.parentUserUuid IS NOT NULL AND LOWER(p.parentUserUuid) = LOWER(:userUuid))
                  OR (lu IS NOT NULL AND lu.id = :userId)
                  OR (co.email IS NOT NULL AND LOWER(co.email) = LOWER(:userEmail))
              )
              AND v.status IN (
                  com.kittyp.visit.enums.VisitStatus.WAITLIST,
                  com.kittyp.visit.enums.VisitStatus.CHECKED_IN,
                  com.kittyp.visit.enums.VisitStatus.IN_PROGRESS,
                  com.kittyp.visit.enums.VisitStatus.CHECKING_OUT,
                  com.kittyp.visit.enums.VisitStatus.COMPLETED,
                  com.kittyp.visit.enums.VisitStatus.CANCELLED,
                  com.kittyp.visit.enums.VisitStatus.NO_SHOW
              )
            ORDER BY v.createdAt DESC
            """)
    List<Visit> findForParentUser(
            @Param("userId") Long userId,
            @Param("userUuid") String userUuid,
            @Param("userEmail") String userEmail,
            @Param("petUuids") List<String> petUuids);

    @EntityGraph(attributePaths = { "pet", "clinicOwner", "doctor", "doctor.user", "clinic" })
    @Query("""
            SELECT DISTINCT v FROM Visit v
            WHERE v.isActive = true
              AND (
                  (v.clinicOwner.linkedUser IS NOT NULL AND v.clinicOwner.linkedUser.id = :userId)
                  OR (v.clinicOwner.email IS NOT NULL AND LOWER(v.clinicOwner.email) = LOWER(:userEmail))
              )
              AND v.status IN (
                  com.kittyp.visit.enums.VisitStatus.WAITLIST,
                  com.kittyp.visit.enums.VisitStatus.CHECKED_IN,
                  com.kittyp.visit.enums.VisitStatus.IN_PROGRESS,
                  com.kittyp.visit.enums.VisitStatus.CHECKING_OUT,
                  com.kittyp.visit.enums.VisitStatus.COMPLETED,
                  com.kittyp.visit.enums.VisitStatus.CANCELLED,
                  com.kittyp.visit.enums.VisitStatus.NO_SHOW
              )
            ORDER BY v.createdAt DESC
            """)
    List<Visit> findForParentByLinkedUserOrEmail(
            @Param("userId") Long userId, @Param("userEmail") String userEmail);

    @EntityGraph(attributePaths = { "pet", "clinicOwner", "doctor", "doctor.user", "clinic" })
    List<Visit> findByClinic_IdAndDoctor_IdOrderByCreatedAtDesc(Long clinicId, Long doctorId);

    @EntityGraph(attributePaths = { "pet", "clinicOwner", "doctor", "doctor.user", "clinic" })
    List<Visit> findByDoctor_IdOrderByCreatedAtDesc(Long doctorId);

    @EntityGraph(attributePaths = { "pet", "clinicOwner", "doctor", "doctor.user", "clinic" })
    List<Visit> findByClinic_IdAndIsActiveTrueAndStatusIn(Long clinicId, Collection<VisitStatus> statuses);

    boolean existsByDoctor_IdAndPet_UuidAndIsActiveTrueAndStatusIn(
            Long doctorId, String petUuid, Collection<VisitStatus> statuses);
}
