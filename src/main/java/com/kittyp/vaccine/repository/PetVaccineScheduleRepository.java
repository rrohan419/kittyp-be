package com.kittyp.vaccine.repository;

import java.time.LocalDate;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.kittyp.vaccine.entity.PetVaccineSchedule;

public interface PetVaccineScheduleRepository extends JpaRepository<PetVaccineSchedule, Long> {

    List<PetVaccineSchedule> findByPet_Uuid(String petUuid);

    List<PetVaccineSchedule> findByCompletedFalseAndDueDateLessThanEqual(LocalDate dueDate);
}
