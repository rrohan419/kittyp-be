package com.kittyp.nutrition.model;

import java.time.LocalDate;
import java.time.LocalTime;

import com.kittyp.nutrition.enums.ItemType;

import lombok.Data;

@Data
public class PetDailyPlanModel {
    
    private String petUuid;

    private String userUuid;

    private String nutritionPlanUuid;

    private ItemType itemType;

    private String itemName;

    private LocalTime time;

    private Double quantityInGrams;

    private String notes;

    private LocalDate day;

    private String timezone = "Asia/Kolkata";

    private boolean notificationSent = false;

    private boolean active = true;

    private int planMonth; // e.g. 11 for November

    private int planYear;
}
