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

    void deactivateExistingPlansForMonth(String petUuid, int month, int year);

    List<PetDailyPlanModel> getPetsDailyPlanActivePlan(String petUuid);


    List<PetDailyPlanModel> createReplaceCurrentMonthPlan(
        String userUuid,
        String petUuid,
        String nutritionPlanUuid,
        List<PetDailyPlanDto> dailyTemplatesDto);
}
