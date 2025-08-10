package com.kittyp.ai.dto;


import lombok.Getter;

@Getter
public class PetNutritionRecommendationDto {
    
    private String uuid;
    private String name;
    private String profilePicture;
    private String type;
    private String breed;
    private String age;
    private String weight;
    private String activityLevel;
    private String gender; 
    private String currentFoodBrand;
    private String healthConditions;
    private String allergies;
}
