package com.kittyp.ai.service;

import com.kittyp.ai.entity.NutritionPlan;
import com.kittyp.ai.model.NutritionPlanModel;
import com.kittyp.ai.model.NutritionRecommendationResponse;
import com.kittyp.common.model.PaginationModel;
import com.kittyp.ai.dto.EnvironmentDataDto;
import com.kittyp.ai.dto.NutritionPlanFilter;

import java.util.concurrent.CompletableFuture;

public interface NutritionPlanService {

    /**
     * Asynchronously save a nutrition plan for a pet
     */
    CompletableFuture<NutritionPlan> saveNutritionPlanAsync(
            String petUuid,
            String userUuid,
            NutritionRecommendationResponse nutritionResponse,
            EnvironmentDataDto environmentData,
            String planName
    );

    /**
     * Get all nutrition plans for a pet
     */
    PaginationModel<NutritionPlanModel> getNutritionPlansForPet(NutritionPlanFilter nutritionPlanFilter, int page, int size);

    /**
     * Set a nutrition plan as active for a pet
     */
    NutritionPlanModel setAsActivePlan(String planUuid, String petUuid);

    /**
     * Delete a nutrition plan
     */
    void deletePlan(String planUuid);

    /**
     * Update plan notes
     */
    NutritionPlan updatePlanNotes(String planUuid, String notes);
}
