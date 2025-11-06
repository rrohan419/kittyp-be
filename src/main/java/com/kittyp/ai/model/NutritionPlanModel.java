package com.kittyp.ai.model;

import java.time.LocalDateTime;
import java.util.List;

import lombok.Data;

@Data
public class NutritionPlanModel {
    
    private String uuid;

    private String planName;

    private Boolean isActivePlan;

    private List<String> tags;

    private String notes;

    private LocalDateTime generationTimestamp;

    private String petUuid;

    private String userUuid;

    private NutritionRecommendationResponse nutritionRecommendationResponse;
}
