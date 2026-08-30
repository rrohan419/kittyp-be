package com.kittyp.vaccine.dao;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Repository;

import com.kittyp.vaccine.entity.PetVaccineSchedule;
import com.kittyp.vaccine.repository.PetVaccineScheduleRepository;

import lombok.RequiredArgsConstructor;

@Repository
@RequiredArgsConstructor
public class PetVaccineScheduleDaoImpl implements PetVaccineScheduleDao {

    private final PetVaccineScheduleRepository petVaccineScheduleRepository;

    @Override
    public List<PetVaccineSchedule> findByPetUuid(String petUuid) {
        return petVaccineScheduleRepository.findByPet_Uuid(petUuid);
    }

    @Override
    public List<PetVaccineSchedule> findDueOnOrBefore(LocalDate date) {
        return petVaccineScheduleRepository.findByCompletedFalseAndDueDateLessThanEqual(date);
    }

    @Override
    public PetVaccineSchedule save(PetVaccineSchedule schedule) {
        return petVaccineScheduleRepository.save(schedule);
    }

    @Override
    public Optional<PetVaccineSchedule> findById(Long id) {
        return petVaccineScheduleRepository.findById(id);
    }
}
