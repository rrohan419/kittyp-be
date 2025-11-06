package com.kittyp.ai.dao;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

import com.kittyp.ai.entity.NutritionPlan;

public interface NutritionPlanDao {
    
    NutritionPlan saveNutritionPlan(NutritionPlan nutritionPlan);

    NutritionPlan getNutritionPlanByUuid(String uuid);

    Page<NutritionPlan> getNutritionPlansByPetUuid(String petUuid, boolean isActive, Pageable pageable);

    List<NutritionPlan> getNutritionPlansByUserUuid(String userUuid, boolean isActive);

    Optional<NutritionPlan> getNutritionPlansByPetUuidAndIsActiveTrue(String petUuid);

    Page<NutritionPlan> findAllByFilter(Specification<NutritionPlan> specification, Pageable pageable);
    
}
