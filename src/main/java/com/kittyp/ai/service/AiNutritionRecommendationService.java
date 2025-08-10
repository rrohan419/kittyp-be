package com.kittyp.ai.service;

import com.kittyp.ai.dto.EnvironmentDataDto;
import com.kittyp.ai.dto.NutritionistRecommendationRequest;
import com.kittyp.ai.dto.PetNutritionRecommendationDto;
import com.kittyp.ai.model.NutritionRecommendationResponse;

import jakarta.servlet.http.HttpServletRequest;

public interface AiNutritionRecommendationService {
    
    NutritionRecommendationResponse generateNutritionRecommendation(PetNutritionRecommendationDto petDetailDto, EnvironmentDataDto environmentData);

    NutritionRecommendationResponse getNutritionRecommendationRequest(NutritionistRecommendationRequest nutritionistRecommendationRequest, HttpServletRequest httpServletRequest);
}
