package com.kittyp.nutrition.service;

import java.util.List;

import com.kittyp.nutrition.dto.PetDailyPlanDto;
import com.kittyp.nutrition.entity.PetDailyPlan;
import com.kittyp.nutrition.model.PetDailyPlanModel;

public interface PetDailyPlanService {
    
    List<PetDailyPlan> createOrReplaceCurrentMonthPlan(
            String userUuid,
            String petUuid,
            String nutritionPlanUuid,
            List<PetDailyPlan> dailyTemplates);

    /** Materialize meal templates across the next {@code days} days (default 30). */
    List<PetDailyPlan> createOrReplaceNextDaysPlan(
            String userUuid,
            String petUuid,
            String nutritionPlanUuid,
            List<PetDailyPlan> dailyTemplates,
            int days);

    void deactivateExistingPlansForMonth(String petUuid, int month, int year);

    List<PetDailyPlanModel> getPetsDailyPlanActivePlan(String petUuid);


    List<PetDailyPlanModel> createReplaceCurrentMonthPlan(
        String userUuid,
        String petUuid,
        String nutritionPlanUuid,
        List<PetDailyPlanDto> dailyTemplatesDto);
}
