package com.kittyp.clinic.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.kittyp.clinic.entity.ClinicInventoryAllocation;

public interface ClinicInventoryAllocationRepository extends JpaRepository<ClinicInventoryAllocation, Long> {

    List<ClinicInventoryAllocation> findByInvoiceUuid(String invoiceUuid);
}
