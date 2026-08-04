package com.kittyp.ai.dto;

import com.kittyp.ai.model.NutritionRecommendationResponse;

import lombok.Getter;

@Getter
public class SaveNutritionPlanDto {

    private String petUuid;

    private String petName;

    private String userUuid;
    
    private EnvironmentDataDto environmentDataDto;

    private NutritionRecommendationResponse recommendationResponse;
}
