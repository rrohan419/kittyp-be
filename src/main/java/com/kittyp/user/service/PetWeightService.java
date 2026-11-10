package com.kittyp.user.service;

import java.time.LocalDateTime;
import java.util.Objects;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.kittyp.common.exception.CustomException;
import com.kittyp.user.entity.Pet;
import com.kittyp.user.entity.PetWeightLog;
import com.kittyp.user.entity.User;
import com.kittyp.user.repository.PetWeightLogRepository;
import com.kittyp.user.repository.PetsRepository;
import com.kittyp.visit.entity.Visit;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class PetWeightService {
    private final PetsRepository petsRepository;
    private final PetWeightLogRepository logs;

    public static double validateWeight(Object value) {
        double weight;
        try {
            weight = Double.parseDouble(value.toString());
        } catch (NumberFormatException ex) {
            throw new CustomException("Weight must be a positive, finite number in kg", HttpStatus.BAD_REQUEST);
        }
        if (!Double.isFinite(weight) || weight <= 0) {
            throw new CustomException("Weight must be a positive, finite number in kg", HttpStatus.BAD_REQUEST);
        }
        return weight;
    }

    @Transactional
    public PetWeightLog recordManual(Pet pet, double weight, LocalDateTime recordedAt, String note, User actor) {
        weight = validateWeight(weight);
        Objects.requireNonNull(actor, "Weight recorder is required");
        Pet locked = lockPet(pet);
        PetWeightLog log = PetWeightLog.builder().pet(locked).weight(weight).recordedAt(recordedAt)
                .note(note).recordedBy(actor).build();
        return saveAndSync(locked, log);
    }

    @Transactional
    public void recordVisit(Visit visit, double weight, User actor) {
        weight = validateWeight(weight);
        Objects.requireNonNull(actor, "Weight recorder is required");
        Pet pet = lockPet(visit.getPet());
        PetWeightLog log = logs.findByVisit_Id(visit.getId()).orElse(null);
        if (log == null) {
            log = PetWeightLog.builder().pet(pet).visit(visit).weight(weight).recordedAt(LocalDateTime.now())
                    .recordedBy(actor).note("Doctor visit at " + visit.getClinic().getName()).build();
        } else if (Double.compare(log.getWeight(), weight) != 0) {
            log.setWeight(weight);
            log.setCorrectedBy(actor);
            log.setCorrectedAt(LocalDateTime.now());
        } else {
            return;
        }
        saveAndSync(pet, log);
    }

    private Pet lockPet(Pet pet) {
        return petsRepository.findForWeightUpdate(pet.getUuid())
                .orElseThrow(() -> new CustomException("Pet not found", HttpStatus.NOT_FOUND));
    }

    private PetWeightLog saveAndSync(Pet pet, PetWeightLog log) {
        PetWeightLog saved = logs.saveAndFlush(log);
        PetWeightLog latest = logs.findFirstByPet_UuidOrderByRecordedAtDescIdDesc(pet.getUuid())
                .orElseThrow(() -> new IllegalStateException("Saved weight log not found"));
        pet.setWeight(String.valueOf(latest.getWeight()));
        petsRepository.save(pet);
        return saved;
    }
}
