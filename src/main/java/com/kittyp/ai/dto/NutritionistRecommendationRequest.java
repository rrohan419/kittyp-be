package com.kittyp.ai.dto;

import com.kittyp.user.dto.LocationData;

import jakarta.validation.Valid;
import lombok.Getter;

@Getter
public class NutritionistRecommendationRequest {
    
    private PetNutritionRecommendationDto petProfile;
    @Valid
    private LocationData location;
}
