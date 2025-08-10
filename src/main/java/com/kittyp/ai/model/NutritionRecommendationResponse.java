package com.kittyp.ai.model;

import java.util.List;
import lombok.Data;

@Data
public class NutritionRecommendationResponse {

    private PetProfileSummary petProfileSummary;
    private EnvironmentalImpact environmentalImpact;
    private DailyFeedingPlan dailyFeedingPlan;
    private List<SpecialConsideration> specialConsiderations;
    private List<RecommendedProduct> recommendedProducts;
    private List<String> longTermWellnessTips;
    private String vetAdviceDisclaimer;
    private Environment environment;

    @Data
    public class Environment {
        private double temperature;
        private String unit;
        private int humidity;
        private String weatherCondition;
        private double windSpeed;
        private String windUnit;
        private int uvIndex;
        private double precipitation;
    }

    @Data
    public static class PetProfileSummary {
        private String name;
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

    @Data
    public static class EnvironmentalImpact {
        private String climateConsiderations;
        private String hydrationNeeds;
        private String energyNeedsAdjustment;
    }

    @Data
    public static class DailyFeedingPlan {
        private int caloriesPerDay;
        private List<Meal> meals;
        private List<Supplement> supplements;

        @Data
        public static class Meal {
            private String time;
            private String foodType;
            private int portionSizeGrams;
            private String notes;
        }

        @Data
        public static class Supplement {
            private String name;
            private String purpose;
            private String dosage;
        }
    }

    @Data
    public static class SpecialConsideration {
        private String condition;
        private String recommendation;
    }

    @Data
    public static class RecommendedProduct {
        private String productName;
        private String category; // food / supplement / accessory
        private String purpose;
        private String url;
    }
}
