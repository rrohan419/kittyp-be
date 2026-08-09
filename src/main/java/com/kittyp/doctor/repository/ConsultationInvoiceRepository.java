package com.kittyp.doctor.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.kittyp.doctor.entity.ConsultationInvoice;

public interface ConsultationInvoiceRepository extends JpaRepository<ConsultationInvoice, Long> {

    Optional<ConsultationInvoice> findByUuid(String uuid);

    Optional<ConsultationInvoice> findByUuidAndDoctor_Id(String uuid, Long doctorId);

    List<ConsultationInvoice> findAllByDoctor_IdOrderByCreatedAtDesc(Long doctorId);
    List<ConsultationInvoice> findAllByOwner_IdOrderByCreatedAtDesc(Long ownerId);

    List<ConsultationInvoice> findAllByClinic_IdOrderByCreatedAtDesc(Long clinicId);

    Optional<ConsultationInvoice> findByUuidAndClinic_Id(String uuid, Long clinicId);

    List<ConsultationInvoice> findAllByOwner_IdAndClinic_IdOrderByCreatedAtDesc(Long ownerId, Long clinicId);

    long countByOwner_Id(Long ownerId);

    long countByOwner_IdAndClinic_Id(Long ownerId, Long clinicId);
}
