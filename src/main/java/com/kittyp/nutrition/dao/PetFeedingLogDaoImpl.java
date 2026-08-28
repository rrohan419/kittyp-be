package com.kittyp.nutrition.dao;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.stereotype.Repository;

import com.kittyp.nutrition.entity.PetFeedingLog;
import com.kittyp.nutrition.repository.PetFeedingLogRepository;

import lombok.RequiredArgsConstructor;

@Repository
@RequiredArgsConstructor
public class PetFeedingLogDaoImpl implements PetFeedingLogDao {

    private final PetFeedingLogRepository petFeedingLogRepository;

    @Override
    public PetFeedingLog save(PetFeedingLog feedingLog) {
        return petFeedingLogRepository.save(feedingLog);
    }

    @Override
    public List<PetFeedingLog> findByPetUuidBetween(String petUuid, LocalDateTime from, LocalDateTime to) {
        return petFeedingLogRepository.findByPetUuidAndLoggedAtBetweenOrderByLoggedAtDesc(petUuid, from, to);
    }

    @Override
    public List<PetFeedingLog> findForDay(String petUuid, LocalDateTime from, LocalDateTime to) {
        return petFeedingLogRepository
                .findByPetUuidAndLoggedAtGreaterThanEqualAndLoggedAtLessThanOrderByLoggedAtDesc(petUuid, from, to);
    }
}
