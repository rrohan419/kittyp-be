package com.kittyp.user.models;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public record PetDashboardModel(
        PetModel pet,
        PetWeightLogModel latestWeight,
        List<VaccineDueModel> openVaccineDues,
        ActiveNutritionPlanModel activeNutritionPlan,
        int todayFeedingCompletionCount,
        TipOfTheDay tipOfTheDay) {

    public record VaccineDueModel(String vaccineName, LocalDate dueDate) {
    }

    public record ActiveNutritionPlanModel(String uuid, String planName, LocalDateTime generatedAt) {
    }

    public record TipOfTheDay(String tip, LocalDate date) {
    }
}
