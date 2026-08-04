package com.kittyp.doctor.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.kittyp.doctor.entity.ConsultationInvoice;

public interface ConsultationInvoiceRepository extends JpaRepository<ConsultationInvoice, Long> {

    Optional<ConsultationInvoice> findByUuid(String uuid);

    Optional<ConsultationInvoice> findByUuidAndDoctor_Id(String uuid, Long doctorId);

    List<ConsultationInvoice> findAllByDoctor_IdOrderByCreatedAtDesc(Long doctorId);
}
