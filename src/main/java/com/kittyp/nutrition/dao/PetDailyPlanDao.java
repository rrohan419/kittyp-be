package com.kittyp.nutrition.dao;

import java.util.List;

import com.kittyp.nutrition.entity.PetDailyPlan;

public interface PetDailyPlanDao {
    
    int deactivatePlansForMonth(String petUuid, int month, int year);

    List<String> petUuidsWithActivePlans();

    List<PetDailyPlan> saveAllPetDailyPlan(List<PetDailyPlan> petDailyPlans);

    List<PetDailyPlan> activePetDailyPlanByPetUuid(String petUuid);
}
