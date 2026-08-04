package com.kittyp.nutrition.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import com.kittyp.nutrition.entity.PetDailyPlan;

public interface PetDailyPlanRepository extends JpaRepository<PetDailyPlan, Long>{
    
    @Modifying
    @Query("UPDATE PetDailyPlan p SET p.active = false WHERE p.petUuid = :petUuid AND p.planMonth = :month AND p.planYear = :year AND p.active = true")
    int deactivatePlansForMonth(String petUuid, int month, int year);

    @Query("SELECT DISTINCT p.petUuid FROM PetDailyPlan p WHERE p.active = true")
    List<String> findDistinctPetUuidsWithActivePlans();

    List<PetDailyPlan> findAllByPetUuidAndIsActiveTrue(String petUuid);
}
