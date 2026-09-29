package com.kittyp.vaccine.dao;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import com.kittyp.vaccine.entity.PetVaccineSchedule;

public interface PetVaccineScheduleDao {

    List<PetVaccineSchedule> findByPetUuid(String petUuid);

    List<PetVaccineSchedule> findDueOnOrBefore(LocalDate date);

    Page<PetVaccineSchedule> findDueOnOrBefore(LocalDate date, Pageable pageable);

    PetVaccineSchedule save(PetVaccineSchedule schedule);

    Optional<PetVaccineSchedule> findById(Long id);
}
