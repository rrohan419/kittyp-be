package com.kittyp.booking.repository;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.kittyp.booking.entity.Booking;
import com.kittyp.booking.enums.BookingStatus;

public interface BookingRepository extends JpaRepository<Booking, Long> {

    Page<Booking> findByClinic_Id(Long clinicId, Pageable pageable);

    Page<Booking> findByClinic_IdAndStatus(Long clinicId, BookingStatus status, Pageable pageable);

    List<Booking> findByClinic_Id(Long clinicId);

    List<Booking> findByDoctor_IdAndSlotStartBetweenOrderBySlotStartAsc(Long doctorId, LocalDateTime from,
            LocalDateTime to);

    List<Booking> findByOwner_IdOrderBySlotStartDesc(Long ownerId);

    @EntityGraph(attributePaths = { "pet", "pet.clinicOwner", "pet.clinicOwner.linkedUser", "doctor", "doctor.user",
            "clinic", "owner" })
    @Query("""
            SELECT DISTINCT b FROM Booking b
            LEFT JOIN b.pet p
            LEFT JOIN p.clinicOwner co
            WHERE b.isActive = true
              AND (
                  (b.owner IS NOT NULL AND b.owner.id = :userId)
                  OR (p IS NOT NULL AND p.uuid IN :petUuids)
                  OR (p.parentUserUuid IS NOT NULL AND LOWER(p.parentUserUuid) = LOWER(:userUuid))
                  OR (co.linkedUser IS NOT NULL AND co.linkedUser.id = :userId)
                  OR (co.email IS NOT NULL AND LOWER(co.email) = LOWER(:userEmail))
              )
            ORDER BY b.slotStart DESC
            """)
    List<Booking> findForParentUser(
            @Param("userId") Long userId,
            @Param("userUuid") String userUuid,
            @Param("userEmail") String userEmail,
            @Param("petUuids") List<String> petUuids);

    @EntityGraph(attributePaths = { "pet", "pet.clinicOwner", "pet.clinicOwner.linkedUser", "doctor", "doctor.user",
            "clinic", "owner" })
    @Query("""
            SELECT DISTINCT b FROM Booking b
            LEFT JOIN b.pet p
            LEFT JOIN p.clinicOwner co
            WHERE b.isActive = true
              AND (
                  (b.owner IS NOT NULL AND b.owner.id = :userId)
                  OR (p.parentUserUuid IS NOT NULL AND LOWER(p.parentUserUuid) = LOWER(:userUuid))
                  OR (co.linkedUser IS NOT NULL AND co.linkedUser.id = :userId)
                  OR (co.email IS NOT NULL AND LOWER(co.email) = LOWER(:userEmail))
              )
            ORDER BY b.slotStart DESC
            """)
    List<Booking> findForParentUserWithoutPets(
            @Param("userId") Long userId,
            @Param("userUuid") String userUuid,
            @Param("userEmail") String userEmail);

    Optional<Booking> findByUuid(String uuid);

    boolean existsByUuid(String uuid);

    @Query("""
            SELECT b FROM Booking b
            WHERE b.doctor.id = :doctorId
              AND b.isActive = true
              AND b.status IN :statuses
              AND b.slotStart < :slotEnd
              AND b.slotEnd > :slotStart
            ORDER BY b.slotStart ASC
            """)
    List<Booking> findOverlappingForDoctor(
            @Param("doctorId") Long doctorId,
            @Param("slotStart") LocalDateTime slotStart,
            @Param("slotEnd") LocalDateTime slotEnd,
            @Param("statuses") Collection<BookingStatus> statuses);
}
