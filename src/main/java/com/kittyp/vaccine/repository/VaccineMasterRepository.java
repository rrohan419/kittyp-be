package com.kittyp.vaccine.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.kittyp.vaccine.entity.VaccineMaster;

public interface VaccineMasterRepository extends JpaRepository<VaccineMaster, Long> {
}
