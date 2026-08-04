package com.kittyp.nutrition.dao;

import java.time.LocalDateTime;
import java.util.List;

import com.kittyp.nutrition.entity.PetFeedingLog;

public interface PetFeedingLogDao {

    PetFeedingLog save(PetFeedingLog feedingLog);

    List<PetFeedingLog> findByPetUuidBetween(String petUuid, LocalDateTime from, LocalDateTime to);

    List<PetFeedingLog> findForDay(String petUuid, LocalDateTime from, LocalDateTime to);
}
