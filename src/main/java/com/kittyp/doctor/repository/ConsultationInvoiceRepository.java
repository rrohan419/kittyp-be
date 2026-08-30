package com.kittyp.doctor.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.kittyp.doctor.entity.ConsultationInvoice;

public interface ConsultationInvoiceRepository extends JpaRepository<ConsultationInvoice, Long> {

    Optional<ConsultationInvoice> findByUuid(String uuid);

    Optional<ConsultationInvoice> findByRazorpayOrderId(String razorpayOrderId);

    boolean existsByUuid(String uuid);

    Optional<ConsultationInvoice> findByUuidAndDoctor_Id(String uuid, Long doctorId);

    List<ConsultationInvoice> findAllByDoctor_IdOrderByCreatedAtDesc(Long doctorId);
    List<ConsultationInvoice> findAllByOwner_IdOrderByCreatedAtDesc(Long ownerId);

    List<ConsultationInvoice> findAllByPetUuidOrderByCreatedAtDesc(String petUuid);

    Page<ConsultationInvoice> findAllByPetUuidOrderByCreatedAtDesc(String petUuid, Pageable pageable);

    List<ConsultationInvoice> findAllByClinic_IdOrderByCreatedAtDesc(Long clinicId);

    Page<ConsultationInvoice> findAllByClinic_IdOrderByCreatedAtDesc(Long clinicId, Pageable pageable);

    List<ConsultationInvoice> findAllByDoctor_IdAndClinic_IdOrderByCreatedAtDesc(Long doctorId, Long clinicId);

    Page<ConsultationInvoice> findAllByDoctor_IdAndClinic_IdOrderByCreatedAtDesc(
            Long doctorId, Long clinicId, Pageable pageable);

    @Query("""
            SELECT i FROM ConsultationInvoice i
            LEFT JOIN i.clinic c
            WHERE i.doctor.id = :doctorId
              AND (c IS NULL OR c.owner.id = :doctorId)
            """)
    Page<ConsultationInvoice> findPersonalPracticeForDoctor(@Param("doctorId") Long doctorId, Pageable pageable);

    @Query("""
            SELECT i FROM ConsultationInvoice i
            WHERE i.doctor.id = :doctorId
              AND (i.clinic.id = :clinicId OR i.clinic IS NULL)
            """)
    Page<ConsultationInvoice> findByDoctorAndClinicOrUnscoped(
            @Param("doctorId") Long doctorId, @Param("clinicId") Long clinicId, Pageable pageable);

    Optional<ConsultationInvoice> findByUuidAndClinic_Id(String uuid, Long clinicId);

    List<ConsultationInvoice> findAllByOwner_IdAndClinic_IdOrderByCreatedAtDesc(Long ownerId, Long clinicId);

    Optional<ConsultationInvoice> findFirstByVisitUuidAndDoctor_IdAndIsActiveTrueOrderByCreatedAtDesc(
            String visitUuid, Long doctorId);

    Optional<ConsultationInvoice> findFirstByVisitUuidAndClinic_IdAndIsActiveTrueOrderByCreatedAtDesc(
            String visitUuid, Long clinicId);

    long countByOwner_Id(Long ownerId);

    long countByOwner_IdAndClinic_Id(Long ownerId, Long clinicId);
}
